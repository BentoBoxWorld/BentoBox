package world.bentobox.bentobox.nms;

import org.bukkit.Chunk;
import org.bukkit.block.data.BlockData;

public interface NMSAbstraction {


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
