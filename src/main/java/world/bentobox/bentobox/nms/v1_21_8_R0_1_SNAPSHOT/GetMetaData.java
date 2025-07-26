package world.bentobox.bentobox.nms.v1_21_8_R0_1_SNAPSHOT;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntity;
import world.bentobox.bentobox.nms.AbstractMetaData;

public class GetMetaData extends AbstractMetaData {

    @Override
    public String nmsData(Block block) {
        Location w = block.getLocation();
        CraftWorld cw = (CraftWorld) w.getWorld(); // CraftWorld is NMS one
        // for 1.13+ (we have use WorldServer)
        TileEntity te = cw.getHandle().c_(new BlockPosition(w.getBlockX(), w.getBlockY(), w.getBlockZ()));
        return getData(te, "getUpdatePacket", "tag");
    }

}