package world.bentobox.bentobox.nms.v1_18_R1;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import world.bentobox.bentobox.nms.BaseNMSAbstraction;


public class NMSHandler extends BaseNMSAbstraction {

    private static final IBlockData AIR = ((CraftBlockData) Bukkit.createBlockData(Material.AIR)).getState();

    @Override
    public void setBlockInNativeChunk(org.bukkit.Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics) {
        CraftBlockData craft = (CraftBlockData) blockData;
        World nmsWorld = ((CraftWorld) chunk.getWorld()).getHandle();
        Chunk nmsChunk = nmsWorld.d(chunk.getX(), chunk.getZ());
        BlockPosition bp = new BlockPosition((chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
        // Setting the block to air before setting to another state prevents some console errors
        nmsChunk.a(bp, AIR, applyPhysics);
        nmsChunk.a(bp, craft.getState(), applyPhysics);
    }

}