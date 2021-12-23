package world.bentobox.bentobox.util;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.nms.NMSAbstraction;

/**
 * Deletes islands chunk by chunk
 *
 * @author tastybento
 */
public class DeleteIslandChunks {

    private int chunkX;
    private int chunkZ;
    private BukkitTask task;
    private final IslandDeletion di;
    private boolean inDelete;
    private final BentoBox plugin;
    private NMSAbstraction nms;
    private final World netherWorld;
    private final World endWorld;
    private final AtomicBoolean completed;

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        this.plugin = plugin;
        this.chunkX = di.getMinXChunk();
        this.chunkZ = di.getMinZChunk();
        this.di = di;
        completed = new AtomicBoolean(false);
        // Nether
        if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
            netherWorld = plugin.getIWM().getNetherWorld(di.getWorld());
        } else {
            netherWorld = null;
        }
        // End
        if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
            endWorld = plugin.getIWM().getEndWorld(di.getWorld());
        } else {
            endWorld = null;
        }
        // NMS
        try {
            this.nms = Util.getNMS();
        } catch (Exception e) {
            plugin.logError("Could not delete chunks because of NMS error");
            return;
        }

        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        regenerateChunks();

    }

    private void regenerateChunks() {
        // Run through all chunks of the islands and regenerate them.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (inDelete) return;
            inDelete = true;
            for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                boolean last = i == plugin.getSettings().getDeleteSpeed() -1;
                final int x = chunkX;
                final int z = chunkZ;
                plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm ->
                // Overworld
                processChunk(gm, di.getWorld(), x, z).thenRun(() ->
                // Nether
                processChunk(gm, netherWorld, x, z).thenRun(() ->
                // End
                processChunk(gm, endWorld, x, z).thenRun(() -> finish(last, x)))));
                chunkZ++;
                if (chunkZ > di.getMaxZChunk()) {
                    chunkZ = di.getMinZChunk();
                    chunkX++;
                }
            }
        }, 0L, 20L);

    }

    private void finish(boolean last, int x) {
        if (x > di.getMaxXChunk()) {
            // Fire event
            IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
            // We're done
            completed.set(true);
            task.cancel();
        }
        if (last) {
            inDelete = false;
        }
    }

    private CompletableFuture<Boolean> processChunk(GameModeAddon gm, World world, int x, int z) {
        if (world != null) {
            CompletableFuture<Boolean> r = new CompletableFuture<>();
            PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> regenerateChunk(r, gm, chunk, x, z));
            return r;
        }
        return CompletableFuture.completedFuture(false);
    }

    private void regenerateChunk(CompletableFuture<Boolean> r, GameModeAddon gm, Chunk chunk, int x, int z) {
        // Clear all inventories
        Arrays.stream(chunk.getTileEntities()).filter(te -> (te instanceof InventoryHolder))
        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
        .forEach(te -> ((InventoryHolder)te).getInventory().clear());
        // Remove all entities
        for (Entity e : chunk.getEntities()) {
            if (!(e instanceof Player)) {
                e.remove();
            }
        }
        // Reset blocks
        MyBiomeGrid grid = new MyBiomeGrid(chunk.getWorld().getEnvironment());
        ChunkGenerator cg = gm.getDefaultWorldGenerator(chunk.getWorld().getName(), "delete");
        // Will be null if use-own-generator is set to true
        if (cg != null) {

            ChunkData cd = cg.generateChunkData(chunk.getWorld(), new Random(), chunk.getX(), chunk.getZ(), grid);
            createChunk(cd, chunk, grid);
        }
        r.complete(true);
    }

    private void createChunk(ChunkData cd, Chunk chunk, MyBiomeGrid grid) {
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (di.inBounds(baseX + x, baseZ + z)) {
                    for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                        nms.setBlockInNativeChunk(chunk, x, y, z, cd.getBlockData(x, y, z), false);
                        // 3D biomes, 4 blocks separated
                        if (x%4 == 0 && y%4 == 0 && z%4 == 0) {
                            chunk.getBlock(x, y, z).setBiome(grid.getBiome(x, y, z));
                        }
                    }
                }
            }
        }
        // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
        Arrays.stream(chunk.getEntities()).filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ())).forEach(Entity::remove);
    }

    public boolean isCompleted() {
        return completed.get();
    }
}
