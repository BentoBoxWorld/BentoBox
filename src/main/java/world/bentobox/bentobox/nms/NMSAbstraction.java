package world.bentobox.bentobox.nms;

import org.bukkit.Chunk;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.BoundingBox;

public interface NMSAbstraction {
    /**
     * Copy the chunk data and biome grid to the given chunk.
     * @param chunk - chunk to copy to
     * @param chunkData - chunk data to copy
     * @param biomeGrid - biome grid to copy to
     * @param limitBox - bounding box to limit the copying
     */
    default void copyChunkDataToChunk(Chunk chunk, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid, BoundingBox limitBox) {
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

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them.
     * @param chunk - chunk to be changed
     * @param x - x coordinate within chunk 0 - 15
     * @param y - y coordinate within chunk 0 - world height, e.g. 255
     * @param z - z coordinate within chunk 0 - 15
     * @param blockData - block data to set the block
     * @param applyPhysics - apply physics or not
     */
    void setBlockInNativeChunk(Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics);

}
