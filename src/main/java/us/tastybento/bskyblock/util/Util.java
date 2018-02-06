package us.tastybento.bskyblock.util;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;

/**
 * A set of utility methods
 *
 * @author Tastybento
 * @author Poslovitch
 */
public class Util {

    private static String serverVersion = null;
    private static BSkyBlock plugin = BSkyBlock.getInstance();

    /**
     * Returns the server version
     * @return
     */
    public static String getServerVersion() {
        if (serverVersion == null) {
            String serverPackageName = plugin.getServer().getClass().getPackage().getName();
            serverVersion = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
        }
        return serverVersion;
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
        if (s == null || s.trim().equals("")) {
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
        List<Type> result = new ArrayList<>();
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
     * Determines if a location is in the island world or not or
     * in the new nether if it is activated
     * @param loc
     * @return true if in the island world
     */
    public static boolean inWorld(Location loc) {
        if (loc != null) {
            if (loc.getWorld().equals(plugin.getIslandWorldManager().getIslandWorld())) {
                return true;
            }
            if (plugin.getSettings().isNetherIslands() && loc.getWorld().equals(plugin.getIslandWorldManager().getNetherWorld())) {
                return true;
            }
            if (plugin.getSettings().isEndIslands() && loc.getWorld().equals(plugin.getIslandWorldManager().getEndWorld())) {
                return true;
            }
        }
        return false;
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
     * Return a list of online players this player can see, i.e. are not invisible
     * @param user - if null, all player names on the server are shown
     * @return
     */
    public static List<String> getOnlinePlayerList(User user) {
        final List<String> returned = new ArrayList<>();
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (user == null || user.getPlayer().canSee(p)) {
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
        final List<String> returned = new ArrayList<>();
        for (String s : list) {
            if (s == null)
                continue;
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }

        return returned;
    }

    /**
     * Loads a YAML file and if it does not exist it is looked for in the JAR
     * 
     * @param file
     * @return
     */
    public static YamlConfiguration loadYamlFile(Plugin plugin, String file) {
        File dataFolder = plugin.getDataFolder();
        File yamlFile = new File(dataFolder, file);

        YamlConfiguration config = null;
        if (yamlFile.exists()) {
            try {
                config = new YamlConfiguration();
                config.load(yamlFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Create the missing file
            config = new YamlConfiguration();
            if (!file.startsWith("players")) {
                plugin.getLogger().info("No " + file + " found. Creating it...");
            }
            try {
                if (plugin.getResource(file) != null) {
                    plugin.getLogger().info("Using default found in jar file.");
                    plugin.saveResource(file, false);
                    config = new YamlConfiguration();
                    config.load(yamlFile);
                } else {
                    config.save(yamlFile);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create the " + file + " file!");
            }
        }
        return config;
    }

    public static void runCommand(final Player player, final String string) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                player.performCommand(string);               
            }});

    }

    /**
     * Get the maximum value of a numerical perm setting
     * @param player - the player to check
     * @param perm - the start of the perm, e.g., bskyblock.maxhomes
     * @param permValue - the default value - the result may be higher or lower than this
     * @return
     */
    public static int getPermValue(Player player, String perm, int permValue) {
        for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
            if (perms.getPermission().startsWith(perm + ".")) {
                // Get the max value should there be more than one
                if (perms.getPermission().contains(perm + ".*")) {
                    return permValue;
                } else {
                    String[] spl = perms.getPermission().split(perm + ".");
                    if (spl.length > 1) {
                        if (!NumberUtils.isDigits(spl[1])) {
                            plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");

                        } else {
                            permValue = Math.max(permValue, Integer.valueOf(spl[1]));
                        }
                    }
                }
            }
            // Do some sanity checking
            if (permValue < 1) {
                permValue = 1;
            }
        }
        return permValue;
    }

}
