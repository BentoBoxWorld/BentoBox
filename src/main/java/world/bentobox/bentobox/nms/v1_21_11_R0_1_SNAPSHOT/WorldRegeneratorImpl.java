package world.bentobox.bentobox.nms.v1_21_11_R0_1_SNAPSHOT;

import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld; // Unversioned import for Paperweight
import org.bukkit.craftbukkit.block.data.CraftBlockData; // Unversioned import for Paperweight

import net.minecraft.core.BlockPos; // New name for BlockPosition
import net.minecraft.world.level.Level; // New name for World
import net.minecraft.world.level.chunk.LevelChunk; // New name for Chunk
import world.bentobox.bentobox.nms.CopyWorldRegenerator;

public class WorldRegeneratorImpl extends CopyWorldRegenerator {

    @Override
    public void setBlockInNativeChunk(org.bukkit.Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics) {
        
        CraftBlockData craft = (CraftBlockData) blockData;
        
        // Unwrap Bukkit World to NMS Level (was World)
        Level nmsWorld = ((CraftWorld) chunk.getWorld()).getHandle();
        
        // Get the NMS Chunk (LevelChunk)
        LevelChunk nmsChunk = nmsWorld.getChunk(chunk.getX(), chunk.getZ());
        
        // Create the NMS Position (BlockPos)
        BlockPos bp = new BlockPos((chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
        
        // Determine the block update flags (1 or 0)
        int flags = applyPhysics ? 1 : 0;
        
        // Setting the block to air before setting to another state prevents some console errors
        nmsChunk.setBlockState(bp, PasteHandlerImpl.AIR, flags);
        
        // Set the desired block state
        nmsChunk.setBlockState(bp, craft.getState(), flags);
    }
}