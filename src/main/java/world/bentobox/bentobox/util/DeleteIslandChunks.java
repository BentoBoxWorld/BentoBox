package world.bentobox.bentobox.util;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.World.Environment;
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
    private IslandDeletion di;
    private boolean inDelete;
    private BentoBox plugin;
    private NMSAbstraction nms;

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        this.plugin = plugin;
        this.chunkX = di.getMinXChunk();
        this.chunkZ = di.getMinZChunk();
        this.di = di;
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
            BentoBox.getInstance().logDebug("Regenerste chunks");
            for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm ->
                // Overworld
                processChunk(gm, Environment.NORMAL, chunkX, chunkZ).thenRun(() ->
                // Nether
                processChunk(gm, Environment.NETHER, chunkX, chunkZ).thenRun(() ->
                // End
                processChunk(gm, Environment.THE_END, chunkX, chunkZ).thenRun(() -> finish()))));
            }
        }, 0L, 20L);

    }

    private void finish() {
        BentoBox.getInstance().logDebug("Finish!");
        chunkZ++;
        if (chunkZ > di.getMaxZChunk()) {
            chunkZ = di.getMinZChunk();
            chunkX++;
            if (chunkX > di.getMaxXChunk()) {
                // We're done
                task.cancel();
                // Fire event
                IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
            }
        }
    }

    private CompletableFuture<Boolean> processChunk(GameModeAddon gm, Environment env, int x, int z) {
        BentoBox.getInstance().logDebug("Game mode " + gm.getDescription().getName());

        World world = di.getWorld();
        BentoBox.getInstance().logDebug("process chunk " + env);
        switch (env) {
        case NETHER:
            // Nether
            if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
                world = plugin.getIWM().getNetherWorld(di.getWorld());
            } else {
                return CompletableFuture.completedFuture(false);
            }
            break;
        case THE_END:
            // End
            if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
                world = plugin.getIWM().getEndWorld(di.getWorld());
            } else {
                return CompletableFuture.completedFuture(false);
            }
            break;
        default:
            break;
        }
        BentoBox.getInstance().logDebug("Check if chunk is generated");
        if (PaperLib.isChunkGenerated(world, x, z)) {
            BentoBox.getInstance().logDebug("Chunk generated");
            PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk ->regenerateChunk(gm, chunk));

            return CompletableFuture.completedFuture(true);
        }
        BentoBox.getInstance().logDebug("Chunk not generated");
        return CompletableFuture.completedFuture(false);
    }

    private void regenerateChunk(GameModeAddon gm, Chunk chunk) {
        BentoBox.getInstance().logDebug("Regenerating chunk " + chunk.getX() + " " + chunk.getZ());
        // Clear all inventories
        Arrays.stream(chunk.getTileEntities()).filter(te -> (te instanceof InventoryHolder))
        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
        .forEach(te -> ((InventoryHolder)te).getInventory().clear());
        // Reset blocks
        MyBiomeGrid grid = new MyBiomeGrid(chunk.getWorld().getEnvironment());
        ChunkGenerator cg = gm.getDefaultWorldGenerator(chunk.getWorld().getName(), "");
        // Will be null if use-own-generator is set to true
        if (cg != null) {

            ChunkData cd = cg.generateChunkData(chunk.getWorld(), new Random(), chunk.getX(), chunk.getZ(), grid);
            createChunk(cd, chunk, grid);
        }

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
        inDelete = false;
    }
}
