package us.tastybento.bskyblock.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.nms.NMSAbstraction;
import us.tastybento.bskyblock.util.placeholders.PlaceholderHandler;

/**
 * A set of utility methods
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class Util {
    private static BSkyBlock plugin = BSkyBlock.getInstance();

    public static void sendMessage(CommandSender receiver, String message){
        message = PlaceholderHandler.replacePlaceholders(receiver, message);
        
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            for(String part : message.split("\n")){
                receiver.sendMessage(part);
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
    public static NMSAbstraction getNMSHandler() throws ClassNotFoundException, IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException {
        String serverPackageName = plugin.getServer().getClass().getPackage().getName();
        String pluginPackageName = plugin.getClass().getPackage().getName();
        String version = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
        Class<?> clazz;
        try {
            clazz = Class.forName(pluginPackageName + ".util.nms." + version + ".NMSHandler");
        } catch (Exception e) {
            plugin.getLogger().info("No NMS Handler found for " + version + ", falling back to Bukkit API.");
            clazz = Class.forName(pluginPackageName + ".util.nms.fallback.NMSHandler");
        }
        // Check if we have a NMSAbstraction implementing class at that location.
        if (NMSAbstraction.class.isAssignableFrom(clazz)) {
            return (NMSAbstraction) clazz.getConstructor().newInstance();
        } else {
            throw new IllegalStateException("Class " + clazz.getName() + " does not implement NMSAbstraction");
        }
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     * 
     * @param s
     *            - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
        if (s == null || s.trim() == "") {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        } else if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
            return new Location(w, x, y, z, yaw, pitch);
        }
        return null;
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     * 
     * @param location
     * @return String of location
     */
    static public String getStringLocation(final Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + Float.floatToIntBits(location.getYaw()) + ":" + Float.floatToIntBits(location.getPitch());
    }
    
    /**
     * Get a list of parameter types for the collection argument in this method
     * @param writeMethod
     * @return
     */
    public static List<Type> getCollectionParameterTypes(Method writeMethod) {
        List<Type> result = new ArrayList<Type>();
        // Get the return type
        // This uses a trick to extract what the arguments are of the writeMethod of the field.
        // In this way, we can deduce what type needs to be written at runtime.
        Type[] genericParameterTypes = writeMethod.getGenericParameterTypes();
        // There could be more than one argument, so step through them
        for (int i = 0; i < genericParameterTypes.length; i++) {
            // If the argument is a parameter, then do something - this should always be true if the parameter is a collection
            if( genericParameterTypes[i] instanceof ParameterizedType ) {
                // Get the actual type arguments of the parameter 
                Type[] parameters = ((ParameterizedType)genericParameterTypes[i]).getActualTypeArguments();
                result.addAll(Arrays.asList(parameters));
            }
        }
        return result;
    }

    public static boolean isOnePointEight() {
        // TODO Auto-generated method stub
        return false;
    }
}
