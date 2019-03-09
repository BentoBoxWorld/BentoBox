package world.bentobox.bentobox.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * A set of utility methods
 *
 * @author tastybento
 * @author Poslovitch
 */
public class Util {

    private static final String NETHER = "_nether";
    private static final String THE_END = "_the_end";
    private static String serverVersion = null;
    private static BentoBox plugin = BentoBox.getInstance();

    private Util() {}

    public static void setPlugin(BentoBox p) {
        plugin = p;
    }

    /**
     * Returns the server version
     * @return server version
     */
    public static String getServerVersion() {
        if (serverVersion == null) {
            String serverPackageName = Bukkit.getServer().getClass().getPackage().getName();
            serverVersion = serverPackageName.substring(serverPackageName.lastIndexOf('.') + 1);
        }
        return serverVersion;
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     *
     * @param location - the location location to query
     * @return Location of closest island
     */
    public static Location getClosestIsland(Location location) {
        int dist = plugin.getIWM().getIslandDistance(location.getWorld());
        long x = Math.round((double) location.getBlockX() / dist) * dist + plugin.getIWM().getIslandXOffset(location.getWorld());
        long z = Math.round((double) location.getBlockZ() / dist) * dist + plugin.getIWM().getIslandZOffset(location.getWorld());
        if (location.getBlockX() == x && location.getBlockZ() == z) {
            return location;
        }
        int y = plugin.getIWM().getIslandHeight(location.getWorld());
        return new Location(location.getWorld(), x, y, z);
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     *
     * @param s - serialized location in format "world:x:y:z:y:p"
     * @return Location
     */
    public static Location getLocationString(final String s) {
        if (s == null || s.trim().equals("")) {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            // Parse string as double just in case
            int x = (int) Double.parseDouble(parts[1]);
            int y = (int) Double.parseDouble(parts[2]);
            int z = (int) Double.parseDouble(parts[3]);
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
            return new Location(w, x + 0.5D, y, z + 0.5D, yaw, pitch);
        }
        return null;
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     * Only stores block ints. Inverse function returns block centers
     *
     * @param l - the location
     * @return String of location in format "world:x:y:z:y:p"
     */
    public static String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
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
        StringBuilder fin = new StringBuilder();
        ugly = ugly.toLowerCase(java.util.Locale.ENGLISH);
        if (ugly.contains("_")) {
            String[] splt = ugly.split("_");
            int i = 0;
            for (String s : splt) {
                i += 1;
                fin.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
                if (i < splt.length) {
                    fin.append(" ");
                }
            }
        } else {
            fin.append(Character.toUpperCase(ugly.charAt(0))).append(ugly.substring(1));
        }
        return fin.toString();
    }

    /**
     * Return a list of online players this player can see, i.e. are not invisible
     * @param user - the User - if null, all player names on the server are shown
     * @return a list of online players this player can see
     */
    public static List<String> getOnlinePlayerList(User user) {
        if (user == null || !user.isPlayer()) {
            // Console and null get to see every player
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        // Otherwise prevent invisible players from seeing
        return Bukkit.getOnlinePlayers().stream().filter(p -> user.getPlayer().canSee(p)).map(Player::getName).collect(Collectors.toList());
    }

    /**
     * Returns all of the items that begin with the given start,
     * ignoring case.  Intended for tabcompletion.
     *
     * @param list - string list
     * @param start - first few chars of a string
     * @return List of items that start with the letters
     */
    public static List<String> tabLimit(final List<String> list, final String start) {
        final List<String> returned = new ArrayList<>();
        for (String s : list) {
            if (s == null) {
                continue;
            }
            if (s.toLowerCase(java.util.Locale.ENGLISH).startsWith(start.toLowerCase(java.util.Locale.ENGLISH))) {
                returned.add(s);
            }
        }

        return returned;
    }

    public static String xyz(Vector location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Checks is world = world2 irrespective of the world type
     * @param world - world
     * @param world2 - world
     * @return true if the same
     */
    public static boolean sameWorld(World world, World world2) {
        String worldName = world.getName().replaceAll(NETHER, "").replaceAll(THE_END, "");
        String world2Name = world2.getName().replaceAll(NETHER, "").replaceAll(THE_END, "");
        return worldName.equalsIgnoreCase(world2Name);
    }

    /**
     * Convert world to an overworld
     * @param world - world
     * @return over world
     */
    public static World getWorld(World world) {
        return world.getEnvironment().equals(Environment.NORMAL) ? world : Bukkit.getWorld(world.getName().replaceAll(NETHER, "").replaceAll(THE_END, ""));
    }

    /**
     * Lists files found in the jar in the folderPath with the suffix given
     * @param jar - the jar file
     * @param folderPath - the path within the jar
     * @param suffix - the suffix required
     * @return a list of files
     */
    public static List<String> listJarFiles(JarFile jar, String folderPath, String suffix) {
        List<String> result = new ArrayList<>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();

            if (!path.startsWith(folderPath)) {
                continue;
            }

            if (entry.getName().endsWith(suffix)) {
                result.add(entry.getName());
            }

        }
        return result;
    }

    /**
     * Converts block face direction to radial degrees. Returns 0 if block face
     * is not radial.
     *
     * @param face - blockface
     * @return degrees
     */
    public static float blockFaceToFloat(BlockFace face) {
        switch (face) {
        case EAST:
            return 90F;
        case EAST_NORTH_EAST:
            return 67.5F;
        case EAST_SOUTH_EAST:
            return 0F;
        case NORTH:
            return 0F;
        case NORTH_EAST:
            return 45F;
        case NORTH_NORTH_EAST:
            return 22.5F;
        case NORTH_NORTH_WEST:
            return 337.5F;
        case NORTH_WEST:
            return 315F;
        case SOUTH:
            return 180F;
        case SOUTH_EAST:
            return 135F;
        case SOUTH_SOUTH_EAST:
            return 157.5F;
        case SOUTH_SOUTH_WEST:
            return 202.5F;
        case SOUTH_WEST:
            return 225F;
        case WEST:
            return 270F;
        case WEST_NORTH_WEST:
            return 292.5F;
        case WEST_SOUTH_WEST:
            return 247.5F;
        default:
            return 0F;
        }
    }

    /**
     * Returns a Date instance corresponding to the input, or null if the input could not be parsed.
     * @param gitHubDate the input to parse
     * @return the Date instance following a {@code yyyy-MM-dd HH:mm:ss} format, or {@code null}.
     * @since 1.3.0
     */
    @Nullable
    public static Date parseGitHubDate(@NonNull String gitHubDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(gitHubDate.replace('T', ' ').replace("Z", ""));
        } catch (ParseException e) {
            return null;
        }
    }


    /**
     * Returns whether this entity is naturally hostile towards the player or not.
     * @param entity the entity to check.
     * @return {@code true} if this entity is hostile, {@code false} otherwise.
     * @since 1.4.0
     */
    public static boolean isHostileEntity(Entity entity) {
        // MagmaCube extends Slime
        // Slime extends Mob
        // Ghast and Phantom extends Flying
        // Flying extends Mob
        // Shulker is Golem, but other Golems cannot be added here.
        // EnderDragon extends LivingEntity
        // Most of hostile mobs extends Monster.
        // PufferFish is a unique fix.

        return entity instanceof Monster || entity instanceof Flying || entity instanceof Slime ||
            entity instanceof Shulker || entity instanceof EnderDragon || entity instanceof PufferFish;
    }


    /**
     * Returns whether this entity is naturally passive towards the player or not.
     * This means that this entity normally won't hurt the player.
     * @param entity the entity to check.
     * @return {@code true} if this entity is passive, {@code false} otherwise.
     * @since 1.4.0
     */
    public static boolean isPassiveEntity(Entity entity) {
        // IronGolem and Snowman extends Golem, but Shulker also extends Golem
        // Fishes, Dolphin and Squid extends WaterMob | Excludes PufferFish
        // Bat extends Mob
        // Most of passive mobs extends Animals

        return entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman ||
            entity instanceof WaterMob && !(entity instanceof PufferFish) || entity instanceof Bat;
    }
}
