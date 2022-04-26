package world.bentobox.bentobox.nms;

import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.MyBiomeGrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public abstract class SimpleNMSAbstraction implements NMSAbstraction {
    private final BentoBox plugin;

    protected SimpleNMSAbstraction() {
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

    @SuppressWarnings("deprecation")
    private CompletableFuture<Void> regenerateChunk(GameModeAddon gm, IslandDeletion di, World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> chunkFuture = PaperLib.getChunkAtAsync(world, chunkX, chunkZ);
        CompletableFuture<Void> invFuture = chunkFuture.thenAccept(chunk ->
                Arrays.stream(chunk.getTileEntities()).filter(InventoryHolder.class::isInstance)
                        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
                        .forEach(te -> ((InventoryHolder) te).getInventory().clear())
        );
        CompletableFuture<Void> entitiesFuture = chunkFuture.thenAccept(chunk -> {
            for (Entity e : chunk.getEntities()) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
        });
        CompletableFuture<Chunk> copyFuture = chunkFuture.thenApply(chunk -> {
            // Reset blocks
            MyBiomeGrid grid = new MyBiomeGrid(chunk.getWorld().getEnvironment());
            ChunkGenerator cg = gm.getDefaultWorldGenerator(chunk.getWorld().getName(), "delete");
            // Will be null if use-own-generator is set to true
            if (cg != null) {
                ChunkGenerator.ChunkData cd = cg.generateChunkData(chunk.getWorld(), new Random(), chunk.getX(), chunk.getZ(), grid);
                copyChunkDataToChunk(chunk, cd, grid, di.getBox());
            }
            return chunk;
        });
        CompletableFuture<Void> postCopyFuture = copyFuture.thenAccept(chunk -> {
            // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
            Arrays.stream(chunk.getEntities()).filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ())).forEach(Entity::remove);
        });
        return CompletableFuture.allOf(invFuture, entitiesFuture, postCopyFuture);
    }

    private void copyChunkDataToChunk(Chunk chunk, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid, BoundingBox limitBox) {
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
                    setBlockInNativeChunk(chunk, x, y, z, chunkData.getBlockData(x, y, z), false);
                    // 3D biomes, 4 blocks separated
                    if (x % 4 == 0 && y % 4 == 0 && z % 4 == 0) {
                        chunk.getBlock(x, y, z).setBiome(biomeGrid.getBiome(x, y, z));
                    }
                }
            }
        }
    }
}
