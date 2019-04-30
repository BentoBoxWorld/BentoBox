package world.bentobox.bentobox.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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

    /**
     * This is how many chunks per world will be done in one tick.
     */
    private static final int SPEED = 5;
    private int x;
    private int z;
    private BukkitTask task;

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        x = di.getMinXChunk();
        z = di.getMinZChunk();
        // Run through all chunks of the islands and regenerate them.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (int i = 0; i < SPEED; i++) {
                plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm -> {

                    regerateChunk(gm, di.getWorld().getChunkAt(x, z));

                    if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
                        regerateChunk(gm, plugin.getIWM().getNetherWorld(di.getWorld()).getChunkAt(x, z));
                    }
                    if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
                        regerateChunk(gm, plugin.getIWM().getEndWorld(di.getWorld()).getChunkAt(x, z));
                    }
                    z++;
                    if (z > di.getMaxZChunk()) {
                        z = di.getMinZChunk();
                        x++;
                        if (x > di.getMaxXChunk()) {
                            // We're done
                            task.cancel();
                            // Fire event
                            IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
                        }
                    }
                });
            }
        }, 0L, 1L);
    }

    private void regerateChunk(GameModeAddon gm, Chunk chunk) {
        // Clear all inventories
        Arrays.stream(chunk.getTileEntities()).filter(te -> (te instanceof InventoryHolder)).forEach(te -> ((InventoryHolder)te).getInventory().clear());
        // Reset blocks
        MyBiomeGrid grid = new MyBiomeGrid();
        ChunkData cd = gm.getDefaultWorldGenerator(chunk.getWorld().getName(), "").generateChunkData(chunk.getWorld(), new Random(), chunk.getX(), chunk.getZ(), grid);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.getBlock(x, 0, z).setBiome(grid.getBiome(x, z));
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                    chunk.getBlock(x, y, z).setBlockData(cd.getBlockData(x, y, z));
                }
            }
        }
        // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
        Arrays.stream(chunk.getEntities()).filter(e -> !(e instanceof Player)).forEach(Entity::remove);
    }

    class MyBiomeGrid implements BiomeGrid {
        Map<Vector, Biome> map = new HashMap<>();
        @Override
        public Biome getBiome(int x, int z) {
            return map.getOrDefault(new Vector(x,0,z), Biome.PLAINS);
        }
        @Override
        public void setBiome(int x, int z, Biome bio) {
            map.put(new Vector(x,0,z), bio);
        }
    }
}
