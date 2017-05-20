package us.tastybento.askyblock.util;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import us.tastybento.askyblock.ASkyBlock;
import us.tastybento.askyblock.util.nms.NMSAbstraction;

/**
 * A set of utility methods
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class Util {
    private static ASkyBlock plugin = ASkyBlock.getInstance();

    public static void sendMessage(CommandSender sender, String message){
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            for(String part : message.split("\n")){
                sender.sendMessage(part);
            }
        }
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return NMSAbstraction class
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static NMSAbstraction checkVersion() throws ClassNotFoundException, IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException {
        String serverPackageName = plugin.getServer().getClass().getPackage().getName();
        String pluginPackageName = plugin.getClass().getPackage().getName();
        String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
        Class<?> clazz;
        try {
            clazz = Class.forName(pluginPackageName + ".nms." + version + ".NMSHandler");
        } catch (Exception e) {
            plugin.getLogger().info("No NMS Handler found for " + version + ", falling back to Bukkit API.");
            clazz = Class.forName(pluginPackageName + ".nms.fallback.NMSHandler");
        }
        // Check if we have a NMSAbstraction implementing class at that location.
        if (NMSAbstraction.class.isAssignableFrom(clazz)) {
            return (NMSAbstraction) clazz.getConstructor().newInstance();
        } else {
            throw new IllegalStateException("Class " + clazz.getName() + " does not implement NMSAbstraction");
        }
    }
}
