package world.bentobox.bentobox.nms.fallback;

import org.bukkit.Chunk;
import org.bukkit.block.data.BlockData;

import world.bentobox.bentobox.nms.CopyWorldRegenerator;

/**
 * @author tastybento
 *
 */
public class WorldRegeneratorImpl extends CopyWorldRegenerator {

    @Override
    protected void setBlockInNativeChunk(Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics) {
        chunk.getBlock(x, y, z).setBlockData(blockData, applyPhysics);
    }


}