package world.bentobox.bentobox.nms;

import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;

public interface NMSAbstraction {
  static final IBlockData AIR =
    ((CraftBlockData) Bukkit.createBlockData(Material.AIR)).getState();

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
  public void setBlockInNativeChunk(
    Chunk chunk,
    int x,
    int y,
    int z,
    BlockData blockData,
    boolean applyPhysics
  );
}
