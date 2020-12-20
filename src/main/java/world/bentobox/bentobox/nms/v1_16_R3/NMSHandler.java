package world.bentobox.bentobox.nms.v1_16_R3;

import net.minecraft.server.v1_16_R3.BlockPosition;
import org.bukkit.Chunk;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import world.bentobox.bentobox.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

  @Override
  public void setBlockInNativeChunk(
    Chunk chunk,
    int x,
    int y,
    int z,
    BlockData blockData,
    boolean applyPhysics
  ) {
    CraftBlockData craft = (CraftBlockData) blockData;
    net.minecraft.server.v1_16_R3.World nmsWorld =
      ((CraftWorld) chunk.getWorld()).getHandle();
    net.minecraft.server.v1_16_R3.Chunk nmsChunk = nmsWorld.getChunkAt(
      chunk.getX(),
      chunk.getZ()
    );
    BlockPosition bp = new BlockPosition(
      (chunk.getX() << 4) + x,
      y,
      (chunk.getZ() << 4) + z
    );
    // Setting the block to air before setting to another state prevents some console errors
    nmsChunk.setType(bp, AIR, applyPhysics, true);
    nmsChunk.setType(bp, craft.getState(), applyPhysics, true);
  }
}
