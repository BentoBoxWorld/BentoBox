package world.bentobox.bentobox.nms;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.block.Block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.level.block.entity.TileEntity;

public abstract class AbstractMetaData {

    public abstract String nmsData(Block block);

    protected String getData(TileEntity te, String method, String field) {
        try {
            // Check if the method 'j' exists
            Method updatePacketMethod = te.getClass().getDeclaredMethod(method);
            // Invoke the method to get the PacketPlayOutTileEntityData object
            updatePacketMethod.setAccessible(true);
            Object object = updatePacketMethod.invoke(te);
            PacketPlayOutTileEntityData packet = (PacketPlayOutTileEntityData) object;
            //if (object instanceof PacketPlayOutTileEntityData packet) {
            // Access the private field for the NBTTagCompound getter in PacketPlayOutTileEntityData
            Field fieldC = packet.getClass().getDeclaredField(field);
            fieldC.setAccessible(true);
            NBTTagCompound nbtTag = (NBTTagCompound) fieldC.get(packet);

            return nbtTag.toString(); // This will show what you want
            //} else {
            //    throw new ClassNotFoundException(
            //            object.getClass().getCanonicalName() + " is not a PacketPlayOutTileEntityData");
            //}
        } catch (Exception e) {
            System.out.println("The method '" + method + "' does not exist in the TileEntity class.");
            e.printStackTrace();
        }
        return "";

    }
}
