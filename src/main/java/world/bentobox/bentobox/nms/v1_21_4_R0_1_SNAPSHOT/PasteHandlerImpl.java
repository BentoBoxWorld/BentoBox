package world.bentobox.bentobox.nms.v1_21_4_R0_1_SNAPSHOT;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.block.data.CraftBlockData;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.DefaultPasteUtil;
import world.bentobox.bentobox.util.Util;

public class PasteHandlerImpl implements PasteHandler {

    protected static final IBlockData AIR = ((CraftBlockData) AIR_BLOCKDATA).getState();

    /**
     * Set the block to the location
     *
     * @param island   - island
     * @param location - location
     * @param bpBlock  - blueprint block
     */
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
            // Set the block data - default is AIR
             CraftBlockData craft = (CraftBlockData) bd;
            net.minecraft.world.level.World nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            Chunk nmsChunk = nmsWorld.d(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            BlockPosition bp = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // Setting the block to air before setting to another state prevents some console errors
            nmsChunk.a(bp, AIR, false);
            nmsChunk.a(bp, craft.getState(), false);
            block.setBlockData(bd, false);
            return block;
    }
}
