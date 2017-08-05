package us.tastybento.bskyblock.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.nms.NMSAbstraction;
import us.tastybento.bskyblock.util.placeholders.PlaceholderHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A set of utility methods
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class Util {
    private static BSkyBlock plugin = BSkyBlock.getPlugin();

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

    /**
     * Results a list of items in a player's hands. Works for older versions of servers
     * @param player
     * @return list of itemstacks
     */
    @SuppressWarnings("deprecation")
    public static List<ItemStack> getPlayerInHandItems(Player player) {
        List<ItemStack> result = new ArrayList<ItemStack>(2);
        if (plugin.getServer().getVersion().contains("(MC: 1.7")
        || plugin.getServer().getVersion().contains("(MC: 1.8")) {
            if (player.getItemInHand() != null)
                result.add(player.getItemInHand());
            return result;
        }
        if (player.getInventory().getItemInMainHand() != null)
            result.add(player.getInventory().getItemInMainHand());
        if (player.getInventory().getItemInOffHand() != null)
            result.add(player.getInventory().getItemInOffHand());
        return result;
    }

    /**
     * Converts a name like IRON_INGOT into Iron Ingot to improve readability
     * 
     * @param ugly
     *            The string such as IRON_INGOT
     * @return A nicer version, such as Iron Ingot
     * 
     *         Credits to mikenon on GitHub!
     */
    public static String prettifyText(String ugly) {
        if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase())))
            return ugly;
        String fin = "";
        ugly = ugly.toLowerCase();
        if (ugly.contains("_")) {
            String[] splt = ugly.split("_");
            int i = 0;
            for (String s : splt) {
                i += 1;
                fin += Character.toUpperCase(s.charAt(0)) + s.substring(1);
                if (i < splt.length)
                    fin += " ";
            }
        } else {
            fin += Character.toUpperCase(ugly.charAt(0)) + ugly.substring(1);
        }
        return fin;
    }

    /**
     * Checks if player has this type of item in either hand
     * @param player
     * @param type
     * @return true if they are holding an item of type type
     */
    @SuppressWarnings("deprecation")
    public static boolean playerIsHolding(Player player, Material type) {
        if (plugin.getServer().getVersion().contains("(MC: 1.7")
        || plugin.getServer().getVersion().contains("(MC: 1.8")) {
            if (player.getItemInHand() != null && player.getItemInHand().getType().equals(type)) {
                return true;
            }
            return false;
        }
        if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType().equals(type)) {
            return true;
        }
        if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInOffHand().getType().equals(type)) {
            return true;
        }
        return false;
    }
    
    /**
     * Display message to player in action bar (1.11+ or chat)
     * @param player
     * @param message
     */
    public static void sendEnterExit(Player player, String message) {
        if (!Settings.showInActionBar
                || plugin.getServer().getVersion().contains("(MC: 1.7")
                || plugin.getServer().getVersion().contains("(MC: 1.8")
                || plugin.getServer().getVersion().contains("(MC: 1.9")
                || plugin.getServer().getVersion().contains("(MC: 1.10")) {
            sendMessage(player, message);
            return;
        }
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                "minecraft:title " + player.getName() + " actionbar {\"text\":\"" + ChatColor.stripColor(message) + "\"}");
    }
    
    /**
     * Determines if a location is in the island world or not or
     * in the new nether if it is activated
     * @param loc
     * @return true if in the island world
     */
    public static boolean inWorld(Location loc) {
        if (loc != null) {
            if (loc.getWorld().equals(IslandWorld.getIslandWorld())) {
                return true;
            }
            if (Settings.netherIslands && loc.getWorld().equals(IslandWorld.getNetherWorld())) {
                return true;
            }
            if (Settings.endIslands && loc.getWorld().equals(IslandWorld.getEndWorld())) {
                return true;
            }
        }
        return true;
    }
    
    /**
     * Determines if an entity is in the island world or not or
     * in the new nether if it is activated
     * @param entity
     * @return
     */
    public static boolean inWorld(Entity entity) {
        return inWorld(entity.getLocation());
    }

    /**
     * Determines if a block is in the island world or not
     * @param block
     * @return true if in the island world
     */
    public static boolean inWorld(Block block) {
        return inWorld(block.getLocation());
    }

    /**
     * One-to-one relationship, you can return the first matched key
     * 
     * @param map
     * @param value
     * @return key
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return a list of online players this player can see, i.e. are not invisible
     * @param player - if null, all player names on the server are shown
     * @return
     */
    public static List<String> getOnlinePlayerList(Player player) {
        final List<String> returned = new ArrayList<String>();
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (player == null) {
                returned.add(p.getName());
            } else if (player.canSee(p)) {
                returned.add(p.getName()); 
            }
        }
        return returned;
    }
    
    /**
     * Returns all of the items that begin with the given start, 
     * ignoring case.  Intended for tabcompletion. 
     * 
     * @param list
     * @param start
     * @return List of items that start with the letters
     */
    public static List<String> tabLimit(final List<String> list, final String start) {
        final List<String> returned = new ArrayList<String>();
        for (String s : list) {
            if (s == null)
                continue;
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }

        return returned;
    }
}
