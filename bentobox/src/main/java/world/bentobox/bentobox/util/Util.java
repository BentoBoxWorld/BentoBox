package world.bentobox.bentobox.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.lib.PaperLib;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult;
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
     * Checks is world = world2 irrespective of the world type. Only strips _nether and _the_end from world name.
     * @param world - world
     * @param world2 - world
     * @return true if the same
     */
    public static boolean sameWorld(World world, World world2) {
        return stripName(world).equals(stripName(world2));
    }

    private static String stripName(World world) {
        if (world.getName().endsWith(NETHER)) {
            return world.getName().substring(0, world.getName().length() - NETHER.length());
        }
        if (world.getName().endsWith(THE_END)) {
            return world.getName().substring(0, world.getName().length() - THE_END.length());
        }
        return world.getName();
    }

    /**
     * Convert world to an overworld
     * @param world - world
     * @return over world
     */
    @Nullable
    public static World getWorld(@Nullable World world) {
        return world == null ? null : world.getEnvironment().equals(Environment.NORMAL) ? world : Bukkit.getWorld(world.getName().replace(NETHER, "").replace(THE_END, ""));
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

    /*
     * PaperLib methods for addons to call
     */

    /**
     * Teleports an Entity to the target location, loading the chunk asynchronously first if needed.
     * @param entity The Entity to teleport
     * @param location The Location to Teleport to
     * @return Future that completes with the result of the teleport
     */
    @Nonnull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location) {
        return PaperLib.teleportAsync(entity, location);
    }

    /**
     * Teleports an Entity to the target location, loading the chunk asynchronously first if needed.
     * @param entity The Entity to teleport
     * @param location The Location to Teleport to
     * @param cause The cause for the teleportation
     * @return Future that completes with the result of the teleport
     */
    @Nonnull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location, TeleportCause cause) {
        return PaperLib.teleportAsync(entity, location, cause);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param loc Location to get chunk for
     * @return Future that completes with the chunk
     */
    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull Location loc) {
        return getChunkAtAsync(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, true);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param loc Location to get chunk for
     * @param gen Should the chunk generate or not. Only respected on some MC versions, 1.13 for CB, 1.12 for Paper
     * @return Future that completes with the chunk, or null if the chunk did not exists and generation was not requested.
     */
    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull Location loc, boolean gen) {
        return getChunkAtAsync(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, gen);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param world World to load chunk for
     * @param x X coordinate of the chunk to load
     * @param z Z coordinate of the chunk to load
     * @return Future that completes with the chunk
     */
    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z) {
        return getChunkAtAsync(world, x, z, true);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param world World to load chunk for
     * @param x X coordinate of the chunk to load
     * @param z Z coordinate of the chunk to load
     * @param gen Should the chunk generate or not. Only respected on some MC versions, 1.13 for CB, 1.12 for Paper
     * @return Future that completes with the chunk, or null if the chunk did not exists and generation was not requested.
     */
    @Nonnull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z, boolean gen) {
        return PaperLib.getChunkAtAsync(world, x, z, gen);
    }

    /**
     * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or any 1.13.1+ version
     * @param loc Location to check if the chunk is generated
     * @return If the chunk is generated or not
     */
    public static boolean isChunkGenerated(@Nonnull Location loc) {
        return isChunkGenerated(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /**
     * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or any 1.13.1+ version
     * @param world World to check for
     * @param x X coordinate of the chunk to check
     * @param z Z coordinate of the chunk to checl
     * @return If the chunk is generated or not
     */
    public static boolean isChunkGenerated(@Nonnull World world, int x, int z) {
        return PaperLib.isChunkGenerated(world, x, z);
    }

    /**
     * Get's a BlockState, optionally not using a snapshot
     * @param block The block to get a State of
     * @param useSnapshot Whether or not to use a snapshot when supported
     * @return The BlockState
     */
    @Nonnull
    public static BlockStateSnapshotResult getBlockState(@Nonnull Block block, boolean useSnapshot) {
        return PaperLib.getBlockState(block, useSnapshot);
    }

    /**
     * Detects if the current MC version is at least the following version.
     *
     * Assumes 0 patch version.
     *
     * @param minor Min Minor Version
     * @return Meets the version requested
     */
    public static boolean isVersion(int minor) {
        return PaperLib.isVersion(minor);
    }

    /**
     * Detects if the current MC version is at least the following version.
     * @param minor Min Minor Version
     * @param patch Min Patch Version
     * @return Meets the version requested
     */
    public static boolean isVersion(int minor, int patch) {
        return PaperLib.isVersion(minor, patch);
    }

    /**
     * Gets the current Minecraft Minor version. IE: 1.13.1 returns 13
     * @return The Minor Version
     */
    public static int getMinecraftVersion() {
        return PaperLib.getMinecraftVersion();
    }

    /**
     * Gets the current Minecraft Patch version. IE: 1.13.1 returns 1
     * @return The Patch Version
     */
    public static int getMinecraftPatchVersion() {
        return PaperLib.getMinecraftPatchVersion();
    }

    /**
     * Check if the server has access to the Spigot API
     * @return True for Spigot <em>and</em> Paper environments
     */
    public static boolean isSpigot() {
        return PaperLib.isSpigot();
    }

    /**
     * Check if the server has access to the Paper API
     * @return True for Paper environments
     */
    public static boolean isPaper() {
        return PaperLib.isPaper();
    }


}
