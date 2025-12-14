package world.bentobox.bentobox.nms.v1_21_11_R0_1_SNAPSHOT;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld; // Unversioned import for Paperweight
import org.bukkit.craftbukkit.block.data.CraftBlockData; // Unversioned import for Paperweight

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState; // New name for IBlockData
import net.minecraft.world.level.chunk.LevelChunk; // New name for Chunk
import net.minecraft.world.level.Level; // New name for net.minecraft.world.level.World

// (Your other imports remain the same)
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.DefaultPasteUtil;
import world.bentobox.bentobox.util.Util;


public class PasteHandlerImpl implements PasteHandler {

    // New type name: BlockState (was IBlockData)
    protected static final BlockState AIR = ((CraftBlockData) AIR_BLOCKDATA).getState();

    // The setBlock(Island, Location, BlueprintBlock) method remains unchanged in its body
    @Override
    public CompletableFuture<Void> setBlock(Island island, Location location, BlueprintBlock bpBlock) {
        return Util.getChunkAtAsync(location).thenRun(() -> {
            Block block = setBlock(location, DefaultPasteUtil.createBlockData(bpBlock));
            DefaultPasteUtil.setBlockState(island, block, bpBlock);
            // Set biome
            if (bpBlock.getBiome() != null) {
                block.setBiome(bpBlock.getBiome());
            }
        });
    }

    @Override
    public Block setBlock(Location location, BlockData bd) {
        Block block = location.getBlock();
        
        // 1. Cast BlockData to CraftBlockData and get NMS BlockState
        CraftBlockData craft = (CraftBlockData) bd;
        BlockState nmsBlockState = craft.getState(); 
        
        // 2. Unwrap Bukkit World to NMS Level
        Level nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
        
        // 3. Get the NMS Chunk (LevelChunk)
        LevelChunk nmsChunk = nmsWorld.getChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        
        // 4. Create the NMS Position (BlockPos)
        BlockPos bp = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                
        // Setting the block to air before setting to another state prevents some console errors...        
        try {
            // New NMS method: nmsChunk.setBlockState(BlockPos, BlockState, int)
            // Setting to air
            nmsChunk.setBlockState(bp, AIR, 0); 
        } catch (Exception e) {
            e.printStackTrace();
            // Ignore
        }
        
        try {
            // Setting the actual block
            nmsChunk.setBlockState(bp, nmsBlockState, 0); 
        } catch (Exception e) {
            e.printStackTrace();
            // Ignore
        }
        
        // The final API call is redundant if the NMS calls succeed, 
        // but often kept as a safeguard in NMS code.
        try {
            block.setBlockData(bd, false);
        } catch (Exception e) {
            e.printStackTrace();
            // Ignore
        }
        
        return block;
    }
}