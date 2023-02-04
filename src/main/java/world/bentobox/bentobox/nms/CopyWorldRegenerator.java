package world.bentobox.bentobox.nms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.IslandDeletion;

/**
 * Regenerates by using a seed world. The seed world is created using the same generator as the game
 * world so that features created by metods like generateNoise or generateCaves can be regenerated.
 * @author tastybento
 *
 */
public abstract class CopyWorldRegenerator implements WorldRegenerator {
    private final BentoBox plugin;

    protected CopyWorldRegenerator() {
        this.plugin = BentoBox.getInstance();
    }

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them.
     *
     * @param chunk        - chunk to be changed
     * @param x            - x coordinate within chunk 0 - 15
     * @param y            - y coordinate within chunk 0 - world height, e.g. 255
     * @param z            - z coordinate within chunk 0 - 15
     * @param blockData    - block data to set the block
     * @param applyPhysics - apply physics or not
     */
    protected abstract void setBlockInNativeChunk(Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics);

    @Override
    public CompletableFuture<Void> regenerate(GameModeAddon gm, IslandDeletion di, World world) {
        CompletableFuture<Void> bigFuture = new CompletableFuture<>();
        new BukkitRunnable() {
            private int chunkX = di.getMinXChunk();
            private int chunkZ = di.getMinZChunk();
            CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

            @Override
            public void run() {
                if (!currentTask.isDone()) return;
                if (isEnded(chunkX)) {
                    cancel();
                    bigFuture.complete(null);
                    return;
                }
                List<CompletableFuture<Void>> newTasks = new ArrayList<>();
                for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                    if (isEnded(chunkX)) {
                        break;
                    }
                    final int x = chunkX;
                    final int z = chunkZ;
                    newTasks.add(regenerateChunk(gm, di, world, x, z));
                    chunkZ++;
                    if (chunkZ > di.getMaxZChunk()) {
                        chunkZ = di.getMinZChunk();
                        chunkX++;
                    }
                }
                currentTask = CompletableFuture.allOf(newTasks.toArray(new CompletableFuture[0]));
            }

            private boolean isEnded(int chunkX) {
                return chunkX > di.getMaxXChunk();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return bigFuture;
    }

    private CompletableFuture<Void> regenerateChunk(GameModeAddon gm, IslandDeletion di, World world, int chunkX, int chunkZ) {

        CompletableFuture<Chunk> seedWorldFuture = getSeedWorldChunk(world, chunkX, chunkZ);

        // Set up a future to get the chunk requests using Paper's Lib. If Paper is used, this should be done async
        CompletableFuture<Chunk> chunkFuture = PaperLib.getChunkAtAsync(world, chunkX, chunkZ);

        CompletableFuture<Void> cleanFuture = cleanChunk(chunkFuture, di);

        CompletableFuture<Void> copyFuture = CompletableFuture.allOf(cleanFuture, chunkFuture, seedWorldFuture);
        
        copyFuture.thenRun(() -> {
            try {
                Chunk chunkToDelete = chunkFuture.get();
                Chunk chunkToCopy = seedWorldFuture.get();
                copyChunkDataToChunk(chunkToDelete, chunkToCopy, di.getBox());

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                
            }
        });
        return CompletableFuture.allOf(cleanFuture, copyFuture);
    }

    private CompletableFuture<Chunk> getSeedWorldChunk(World world, int chunkX, int chunkZ) {
        World seed = Bukkit.getWorld("seeds/" + world.getName());
        if (seed == null) return CompletableFuture.completedFuture(null);     
        return PaperLib.getChunkAtAsync(seed, chunkX, chunkZ);
    }

    /**
     * Cleans up the chunk of inventories and entities
     * @param chunkFuture the future chunk to be cleaned
     * @param di island deletion data
     * @return future completion of this task
     */
    private CompletableFuture<Void> cleanChunk(CompletableFuture<Chunk> chunkFuture, IslandDeletion di) {
        // when it is complete, then run through all the tile entities in the chunk and clear them, e.g., chests are emptied
        CompletableFuture<Void> invFuture = chunkFuture.thenAccept(chunk ->
        Arrays.stream(chunk.getTileEntities()).filter(InventoryHolder.class::isInstance)
        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
        .forEach(te -> ((InventoryHolder) te).getInventory().clear())
                );
        
        // Similarly, when the chunk is loaded, remove all the entities in the chunk apart from players
        CompletableFuture<Void> entitiesFuture = chunkFuture.thenAccept(chunk -> {
            // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
            Arrays.stream(chunk.getEntities())
            .filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ()))
            .forEach(Entity::remove);
        });
        return CompletableFuture.allOf(invFuture, entitiesFuture);
    }

    private void copyChunkDataToChunk(Chunk chunk, Chunk chunkData, BoundingBox limitBox) {
        double baseX = chunk.getX() << 4;
        double baseZ = chunk.getZ() << 4;
        int minHeight = chunk.getWorld().getMinHeight();
        int maxHeight = chunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!limitBox.contains(baseX + x, 0, baseZ + z)) {
                    continue;
                }
                for (int y = minHeight; y < maxHeight; y++) {
                    setBlockInNativeChunk(chunk, x, y, z, chunkData.getBlock(x, y, z).getBlockData(), false);
                    // 3D biomes, 4 blocks separated
                    if (x % 4 == 0 && y % 4 == 0 && z % 4 == 0) {
                        chunk.getBlock(x, y, z).setBiome(chunkData.getBlock(x, y, z).getBiome());
                    }
                }
            }
        }
        // TODO: Copy chest contents and entities
    }
}
