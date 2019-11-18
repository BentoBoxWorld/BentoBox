package world.bentobox.bentobox.util;

import java.util.Arrays;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
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

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();

        this.chunkX = di.getMinXChunk();
        this.chunkZ = di.getMinZChunk();
        this.di = di;
        // Run through all chunks of the islands and regenerate them.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (inDelete) return;
            inDelete = true;
            for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm -> {
                    // Overworld
                    processChunk(gm, di.getWorld(), chunkX, chunkZ);
                    // Nether
                    if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
                        processChunk(gm, plugin.getIWM().getNetherWorld(di.getWorld()), chunkX, chunkZ);
                    }
                    // End
                    if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
                        processChunk(gm, plugin.getIWM().getEndWorld(di.getWorld()), chunkX, chunkZ);
                    }
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
                });
            }
            inDelete = false;
        }, 0L, 20L);
    }

    private void processChunk(GameModeAddon gm, World world, int x, int z) {
        if (PaperLib.isChunkGenerated(world, x, z)) {
            PaperLib.getChunkAtAsync(world, x, z).thenAccept(chunk -> regenerateChunk(gm, chunk));
        }
    }

    private void regenerateChunk(GameModeAddon gm, Chunk chunk) {
        boolean isLoaded = chunk.isLoaded();
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
            int baseX = chunk.getX() << 4;
            int baseZ = chunk.getZ() << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (di.inBounds(baseX + x, baseZ + z)) {
                        chunk.getBlock(x, 0, z).setBiome(grid.getBiome(x, z));
                        for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                            // Note: setting block to air before setting it to something else stops a bug in the server
                            // where it reports a "
                            chunk.getBlock(x, y, z).setType(Material.AIR, false);
                            chunk.getBlock(x, y, z).setBlockData(cd.getBlockData(x, y, z), false);
                        }
                    }
                }
            }
        }
        // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
        Arrays.stream(chunk.getEntities()).filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ())).forEach(Entity::remove);
        if (!isLoaded) {
            chunk.unload(true);
        }
    }
}
