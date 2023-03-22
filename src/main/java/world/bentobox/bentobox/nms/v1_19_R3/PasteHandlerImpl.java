package world.bentobox.bentobox.nms.v1_19_R3;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.chunk.Chunk;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.DefaultPasteUtil;
import world.bentobox.bentobox.util.Util;

public class PasteHandlerImpl extends world.bentobox.bentobox.nms.v1_19_R2.PasteHandlerImpl {
       
    /**
     * Set the block to the location
     *
     * @param island   - island
     * @param location - location
     * @param bpBlock  - blueprint block
     */
    public static CompletableFuture<Void> setBlock(Island island, Location location, BlueprintBlock bpBlock) {
        return Util.getChunkAtAsync(location).thenRun(() -> {
            Block block = location.getBlock();
            // Set the block data - default is AIR
            BlockData bd = DefaultPasteUtil.createBlockData(bpBlock);
            CraftBlockData craft = (CraftBlockData) bd;
            net.minecraft.world.level.World nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            Chunk nmsChunk = nmsWorld.d(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            BlockPosition bp = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // Setting the block to air before setting to another state prevents some console errors
            nmsChunk.a(bp, AIR, false);
            nmsChunk.a(bp, craft.getState(), false);
            block.setBlockData(bd, false);
            DefaultPasteUtil.setBlockState(island, block, bpBlock);
            // Set biome
            if (bpBlock.getBiome() != null) {
                block.setBiome(bpBlock.getBiome());
            }
        });
    }
}
