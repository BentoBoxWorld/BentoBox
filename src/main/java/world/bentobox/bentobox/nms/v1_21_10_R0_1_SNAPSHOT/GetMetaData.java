package world.bentobox.bentobox.nms.v1_21_10_R0_1_SNAPSHOT;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import world.bentobox.bentobox.nms.AbstractMetaData;


public class GetMetaData extends AbstractMetaData {

    @Override
    public String nmsData(Block block) {
        ServerLevel sl = ((CraftWorld) block.getWorld()).getHandle();
        BlockEntity te = sl.getBlockEntity(new BlockPos(block.getX(), block.getY(), block.getZ()));

        ClientboundBlockEntityDataPacket packet = (ClientboundBlockEntityDataPacket) te.getUpdatePacket();
        CompoundTag nbtTag = packet.getTag();

        return nbtTag.toString();
    }
}