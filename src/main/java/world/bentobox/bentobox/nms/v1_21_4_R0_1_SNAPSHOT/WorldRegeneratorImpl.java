package world.bentobox.bentobox.nms.v1_21_4_R0_1_SNAPSHOT;

import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.block.data.CraftBlockData;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.Chunk;
import world.bentobox.bentobox.nms.CopyWorldRegenerator;

public class WorldRegeneratorImpl extends CopyWorldRegenerator {

    @Override
    public void setBlockInNativeChunk(org.bukkit.Chunk chunk, int x, int y, int z, BlockData blockData,
            boolean applyPhysics) {
        CraftBlockData craft = (CraftBlockData) blockData;
        World nmsWorld = ((CraftWorld) chunk.getWorld()).getHandle();
        Chunk nmsChunk = nmsWorld.d(chunk.getX(), chunk.getZ());
        BlockPosition bp = new BlockPosition((chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
        // Setting the block to air before setting to another state prevents some console errors
        nmsChunk.a(bp, PasteHandlerImpl.AIR, applyPhysics);
        nmsChunk.a(bp, craft.getState(), applyPhysics);
    }

}