package world.bentobox.bentobox.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Allay;
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
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.nms.AbstractMetaData;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.nms.WorldRegenerator;


/**
 * A set of utility methods
 *
 * @author tastybento
 * @author Poslovitch
 */
public class Util {
    /**
     * Use standard color code definition: {@code &<hex>}.
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})");

    private static final String NETHER = "_nether";
    private static final String THE_END = "_the_end";
    private static String serverVersion = null;
    private static BentoBox plugin = BentoBox.getInstance();
    private static PasteHandler pasteHandler = null;
    private static WorldRegenerator regenerator = null;

    private static AbstractMetaData metaData;

    private Util() {}

    /**
     * Used for testing only
     */
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
        int dist = plugin.getIWM().getIslandDistance(location.getWorld()) * 2;
        long x = Math.round((double) location.getBlockX() / dist) * dist + plugin.getIWM().getIslandXOffset(location.getWorld());
        long z = Math.round((double) location.getBlockZ() / dist) * dist + plugin.getIWM().getIslandZOffset(location.getWorld());
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
            final World w = Bukkit.getWorld(parts[0]);
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
     * Return an immutable list of online players this player can see, i.e. are not invisible
     * @param user - the User - if null, all player names on the server are shown
     * @return a list of online players this player can see
     */
    public static List<String> getOnlinePlayerList(User user) {
        if (user == null || !user.isPlayer()) {
            // Console and null get to see every player
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        // Otherwise prevent invisible players from seeing
        return Bukkit.getOnlinePlayers().stream().filter(p -> user.getPlayer().canSee(p)).map(Player::getName).toList();
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
     * @return over world or null if world is null or a world cannot be found
     */
    @Nullable
    public static World getWorld(@Nullable World world) {
        if (world == null) {
            return null;
        }
        return world.getEnvironment().equals(Environment.NORMAL) ? world : Bukkit.getWorld(world.getName().replace(NETHER, "").replace(THE_END, ""));
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
        return switch (face) {
        case EAST -> 90F;
        case EAST_NORTH_EAST -> 67.5F;
        case NORTH_EAST -> 45F;
        case NORTH_NORTH_EAST -> 22.5F;
        case NORTH_NORTH_WEST -> 337.5F;
        case NORTH_WEST -> 315F;
        case SOUTH -> 180F;
        case SOUTH_EAST -> 135F;
        case SOUTH_SOUTH_EAST -> 157.5F;
        case SOUTH_SOUTH_WEST -> 202.5F;
        case SOUTH_WEST -> 225F;
        case WEST -> 270F;
        case WEST_NORTH_WEST -> 292.5F;
        case WEST_SOUTH_WEST -> 247.5F;
        default -> 0F;
        };
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
                entity instanceof WaterMob && !(entity instanceof PufferFish) || entity instanceof Bat ||
                entity instanceof Allay;
    }

    public static boolean isTamableEntity(Entity entity) {
        return entity instanceof Tameable && ((Tameable) entity).isTamed();
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
    @NonNull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location) {
        return teleportAsync(entity, location, TeleportCause.UNKNOWN);
    }

    /**
     * Teleports an Entity to the target location, loading the chunk asynchronously first if needed.
     * @param entity The Entity to teleport
     * @param location The Location to Teleport to
     * @param cause The cause for the teleportation
     * @return Future that completes with the result of the teleport
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static CompletableFuture<Boolean> teleportAsync(@Nonnull Entity entity, @Nonnull Location location,
            TeleportCause cause) {
        try {
            // Use reflection to check if the method exists
            Method method = Entity.class.getMethod("teleportAsync", Location.class, TeleportCause.class);
            if (method != null) {
                // Invoke the method using reflection on the entity instance
                return (CompletableFuture<Boolean>) method.invoke(entity, location, cause);
            }
        } catch (NoSuchMethodException e) {
            // Method does not exist, fallback to Spigot behavior
        } catch (Exception e) {
            plugin.logStacktrace(e); // Report other exceptions
        }
        // Fallback for Spigot servers
        entity.teleport(location, cause);
        return CompletableFuture.completedFuture(true);
    }
    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param loc Location to get chunk for
     * @return Future that completes with the chunk
     */
    @NonNull
    public static CompletableFuture<Chunk> getChunkAtAsync(@NonNull Location loc) {
        return getChunkAtAsync(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4,
                true);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param loc Location to get chunk for
     * @param gen Should the chunk generate or not. Only respected on some MC versions, 1.13 for CB, 1.12 for Paper
     * @return Future that completes with the chunk, or null if the chunk did not exists and generation was not requested.
     */
    @NonNull
    public static CompletableFuture<Chunk> getChunkAtAsync(@NonNull Location loc, boolean gen) {
        return getChunkAtAsync(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4, gen);
    }

    /**
     * Gets the chunk at the target location, loading it asynchronously if needed.
     * @param world World to load chunk for
     * @param x X coordinate of the chunk to load
     * @param z Z coordinate of the chunk to load
     * @return Future that completes with the chunk
     */
    @NonNull
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
    @SuppressWarnings("unchecked")
    @NonNull
    public static CompletableFuture<Chunk> getChunkAtAsync(@Nonnull World world, int x, int z, boolean gen) {
        try {
            // Use reflection to check if the method exists
            Method method = World.class.getMethod("getChunkAtAsync", int.class, int.class, boolean.class);
            if (method != null) {
                // Invoke the method using reflection
                return (CompletableFuture<Chunk>) method.invoke(world, x, z, gen);
            }
        } catch (NoSuchMethodException e) {
            // Method does not exist, fallback to Spigot behavior
        } catch (Exception e) {
            e.printStackTrace(); // Handle other exceptions (optional)
        }
        // Fallback for Spigot servers
        return CompletableFuture.completedFuture(world.getChunkAt(x, z, gen));

    }

    /**
     * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or any 1.13.1+ version
     * @param loc Location to check if the chunk is generated
     * @return If the chunk is generated or not
     */
    public static boolean isChunkGenerated(@NonNull Location loc) {
        return isChunkGenerated(Objects.requireNonNull(loc.getWorld()), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /**
     * Checks if the chunk has been generated or not. Only works on Paper 1.12+ or any 1.13.1+ version
     * @param world World to check for
     * @param x X coordinate of the chunk to check
     * @param z Z coordinate of the chunk to checl
     * @return If the chunk is generated or not
     */
    public static boolean isChunkGenerated(@Nonnull World world, int x, int z) {
        return world.isChunkGenerated(x, z);
    }

    /**
     * Checks if the given version is compatible with the required version.
     * 
     * <p>
     * A version is considered compatible if:
     * <ul>
     *   <li>The major, minor, and patch components of the given version are greater than or equal to those of the required version.</li>
     *   <li>If the numeric components are equal, the absence of "-SNAPSHOT" in the given version takes precedence (i.e., release versions are considered more compatible than SNAPSHOT versions).</li>
     * </ul>
     * </p>
     * 
     * @param version          the version to check, in the format "major.minor.patch[-SNAPSHOT]".
     * @param requiredVersion  the required version, in the format "major.minor.patch[-SNAPSHOT]".
     * @return {@code true} if the given version is compatible with the required version; {@code false} otherwise.
     * 
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code isVersionCompatible("2.1.0", "2.0.0-SNAPSHOT")} returns {@code true}</li>
     *   <li>{@code isVersionCompatible("2.0.0", "2.0.0-SNAPSHOT")} returns {@code true}</li>
     *   <li>{@code isVersionCompatible("2.0.0-SNAPSHOT", "2.0.0")} returns {@code false}</li>
     *   <li>{@code isVersionCompatible("1.9.9", "2.0.0-SNAPSHOT")} returns {@code false}</li>
     * </ul>
     * </p>
     */
    public static boolean isVersionCompatible(String version, String requiredVersion) {
        String[] versionParts = version.replace("-SNAPSHOT", "").split("\\.");
        String[] requiredVersionParts = requiredVersion.replace("-SNAPSHOT", "").split("\\.");

        for (int i = 0; i < Math.max(versionParts.length, requiredVersionParts.length); i++) {
            int vPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int rPart = i < requiredVersionParts.length ? Integer.parseInt(requiredVersionParts[i]) : 0;

            if (vPart > rPart) {
                return true;
            } else if (vPart < rPart) {
                return false;
            }
        }

        // If numeric parts are equal, prioritize SNAPSHOT as lower precedence
        boolean isVersionSnapshot = version.contains("-SNAPSHOT");
        boolean isRequiredSnapshot = requiredVersion.contains("-SNAPSHOT");

        // If required version is a full release but current version is SNAPSHOT, it's incompatible
        return !(!isRequiredSnapshot && isVersionSnapshot);
    }

    /**
     * Check if the server has access to the Paper API
     * @return True for Paper environments
     */
    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true; // Paper-specific class exists
        } catch (ClassNotFoundException e) {
            return false; // Not a Paper server
        }
    }

    /**
     * This method translates color codes in given string and strips whitespace after them.
     * This code parses both: hex and old color codes.
     * @param textToColor Text which color codes must be parsed.
     * @return String text with parsed colors and stripped whitespaces after them.
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public static String translateColorCodes(@NonNull String textToColor) {
        // Use matcher to find hex patterns in given text.
        Matcher matcher = HEX_PATTERN.matcher(textToColor);
        // Increase buffer size by 32 like it is in bungee cord api. Use buffer because it is sync.
        StringBuilder buffer = new StringBuilder(textToColor.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);

            if (group.length() == 6) {
                // Parses #ffffff to a color text.
                matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                        + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
                        + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
                        + ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5));
            } else {
                // Parses #fff to a color text.
                matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                        + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(0)
                        + ChatColor.COLOR_CHAR + group.charAt(1) + ChatColor.COLOR_CHAR + group.charAt(1)
                        + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(2));
            }
        }

        // transform normal codes and strip spaces after color code.
        return Util.stripSpaceAfterColorCodes(
                ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString()));
    }


    /**
     * Strips spaces immediately after color codes. Used by {@link User#getTranslation(String, String...)}.
     * @param textToStrip - text to strip
     * @return text with spaces after color codes removed
     * @since 1.9.0
     */
    @NonNull
    public static String stripSpaceAfterColorCodes(@NonNull String textToStrip) {
        if (textToStrip == null) return "";
        textToStrip = textToStrip.replaceAll("(" + ChatColor.COLOR_CHAR + ".)[\\s]", "$1");
        return textToStrip;
    }

    /**
     * Returns whether the input is an integer or not.
     * @param nbr the input.
     * @param parse whether the input should be checked to ensure it can be parsed as an Integer without throwing an exception.
     * @return {@code true} if the input is an integer, {@code false} otherwise.
     * @since 1.10.0
     */
    public static boolean isInteger(@NonNull String nbr, boolean parse) {
        // Original code from Jonas Klemming on StackOverflow (https://stackoverflow.com/q/237159).
        // I slightly refined it to catch more edge cases.
        // It is a faster alternative to catch malformed strings than the NumberFormatException.
        int length = nbr.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (nbr.charAt(0) == '-' || nbr.charAt(0) == '+') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        boolean trailingDot = false;
        for (; i < length; i++) {
            char c = nbr.charAt(i);
            if (trailingDot && c != '0') {
                // We only accept 0's after a trailing dot.
                return false;
            }
            if (c == '.') {
                if (i == length - 1) {
                    // We're at the end of the integer, so it's okay
                    return true;
                } else {
                    // we will need to make sure there is nothing else but 0's after the dot.
                    trailingDot = true;
                }
            } else if (!trailingDot && (c < '0' || c > '9')) {
                return false;
            }
        }

        // these tests above should have caught most likely issues
        // We now need to make sure parsing the input as an Integer won't cause issues
        if (parse) {
            try {
                Integer.parseInt(nbr); // NOSONAR we don't care about the result of this operation
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Everything's green!
        return true;
    }

    /**
     * Get a UUID from a string. The string can be a known player's name or a UUID
     * @param nameOrUUID - name or UUID
     * @return UUID or null if unknown
     * @since 1.13.0
     */
    @Nullable
    public static UUID getUUID(@NonNull String nameOrUUID) {
        UUID targetUUID = plugin.getPlayers().getUUID(nameOrUUID);
        if (targetUUID != null) return targetUUID;
        // Check if UUID is being used
        try {
            return UUID.fromString(nameOrUUID);
        } catch (Exception e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Run a list of commands for a user
     * @param user - user affected by the commands
     * @param commands - a list of commands
     * @param commandType - the type of command being run - used in the console error message
     */
    public static void runCommands(User user, @NonNull List<String> commands, String commandType) {
        runCommands(user, user.getName(), commands, commandType);
    }

    /**
     * Run a list of commands for a user
     * @param user - user affected by the commands
     * @param ownerName - name of the island owner, or the user's name if it is the user's island
     * @param commands - a list of commands
     * @param commandType - the type of command being run - used in the console error message
     * @since 1.22.0
     */
    public static void runCommands(User user, String ownerName, @NonNull List<String> commands, String commandType) {
        commands.forEach(command -> {
            command = command.replace("[player]", user.getName());
            command = command.replace("[owner]", ownerName);
            if (command.startsWith("[SUDO]")) {
                // Execute the command by the player
                if (!user.isOnline() || !user.performCommand(command.substring(6))) {
                    plugin.logError("Could not execute " + commandType + " command for " + user.getName() + ": " + command.substring(6));
                }
            } else {
                // Otherwise execute as the server console
                if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                    plugin.logError("Could not execute " + commandType + " command as console: " + command);
                }
            }
        });

    }

    /**
     * Resets the player's heath to maximum
     * @param player - player
     */
    public static void resetHealth(Player player) {
        try {
            // Paper
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            player.setHealth(maxHealth);
        } catch (Exception e) {
            // Spigot
            player.setHealth(20D);
        }
    }

    /**
     * Set the regenerator the plugin will use
     * @param regenerator the regenerator
     */
    public static void setRegenerator(WorldRegenerator regenerator) {
        Util.regenerator = regenerator;
    }
    
    private static Pair<String, String> getPrefix() {
        // Bukkit method that was added in 2011
        // Example value: 1.20.4-R0.1-SNAPSHOT
        final String bukkitVersion = "v" + Bukkit.getBukkitVersion().replace('.', '_').replace('-', '_');
        final String pluginPackageName = plugin.getClass().getPackage().getName();
        return new Pair<String, String>(pluginPackageName + ".nms." + bukkitVersion, bukkitVersion);
    }
    
    /**
     * Generic method to get NMS handlers with fallback options
     * @param <T> The type of handler to get
     * @param handlerClass The class of the handler
     * @param implName The implementation name (e.g., "PasteHandlerImpl")
     * @param fallbackSupplier Supplier for the fallback implementation
     * @param existingHandler The existing handler instance if any
     * @param logPrefix Prefix for logging messages
     * @return The handler instance
     */
    private static <T> T getNMSHandler(Class<T> handlerClass, 
            String implName, 
            java.util.function.Supplier<T> fallbackSupplier,
            T existingHandler,
            String logPrefix) {
        if (existingHandler != null) {
            return existingHandler;
        }
        
        T handler;
        try {
            Class<?> clazz = Class.forName(getPrefix().x + "." + implName);
            if (handlerClass.isAssignableFrom(clazz)) {
                handler = handlerClass.cast(clazz.getConstructor().newInstance());
            } else {
                throw new IllegalStateException("Class " + clazz.getName() + " does not implement " + handlerClass.getSimpleName());
            }
        } catch (Exception e) {
            plugin.logWarning("No " + logPrefix + " found for " + getPrefix().z + ", falling back to Bukkit API.");
            handler = fallbackSupplier.get();
        }
        return handler;
    }
    
    /**
     * Get metadata decoder
     * @return an accelerated metadata class for this server
     */
    public static AbstractMetaData getMetaData() {
        if (metaData == null) {
            metaData = getNMSHandler(AbstractMetaData.class, 
                    "GetMetaData",
                    () -> new world.bentobox.bentobox.nms.fallback.GetMetaData(),
                    metaData,
                    "GetMetaData");
        }
        return metaData;
    }

    /**
     * Get the regenerator the plugin will use
     * @return an accelerated regenerator class for this server
     */
    public static WorldRegenerator getRegenerator() {
        if (regenerator == null) {
            regenerator = getNMSHandler(WorldRegenerator.class,
                    "WorldRegeneratorImpl",
                    () -> new world.bentobox.bentobox.nms.fallback.WorldRegeneratorImpl(),
                    regenerator,
                    "Regenerator");
        }
        return regenerator;
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return PasteHandler
     */
    public static PasteHandler getPasteHandler() {
        if (pasteHandler == null) {
            BentoBox.getInstance().log("Optimizing for " + getPrefix().z);
            pasteHandler = getNMSHandler(PasteHandler.class,
                    "PasteHandlerImpl",
                    () -> new world.bentobox.bentobox.nms.fallback.PasteHandlerImpl(),
                    pasteHandler,
                    "PasteHandler");
        }
        return pasteHandler;
    }

    /**
     * Broadcast a localized message to all players with the permission {@link Server#BROADCAST_CHANNEL_USERS}
     *
     * @param localeKey locale key for the message to broadcast
     * @param variables any variables for the message
     * @return number of message recipients
     */
    public static int broadcast(String localeKey, String... variables) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(Server.BROADCAST_CHANNEL_USERS)) {
                User.getInstance(p).sendMessage(localeKey, variables);
                count++;
            }
        }
        return count;
    }


    /**
     * This method removes all special characters that are not allowed in filenames (windows).
     * It also includes any white-spaces, as for some reason, I do like it more without them.
     * Also, all cases are lower cased for easier blueprint mapping.
     * @param input Input that need to be sanitized.
     * @return A sanitized input without illegal characters in names.
     */
    @SuppressWarnings("deprecation")
    public static String sanitizeInput(String input)
    {
        return Util.stripColor(
                Util.translateColorCodes(input.replaceAll("[\\\\/:*?\"<>|\s]", "_"))).
                toLowerCase();
    }

    /**
     * Attempts to find the first matching enum constant from an array of possible string representations.
     * This method sequentially checks each string against the enum constants of the specified enum class
     * by normalizing the string values to uppercase before comparison, enhancing the likelihood of a match
     * if the enum constants are defined in uppercase.
     *
     * @param enumClass the Class object of the enum type to be checked against
     * @param values an array of string values which are potential matches for the enum constants
     * @param <T> the type parameter of the enum
     * @return the first matching enum constant if a match is found; otherwise, returns null
     * @throws IOException 
     * @throws NullPointerException if either {@code enumClass} or {@code values} are null
     */
    public static <T extends Enum<T>> T findFirstMatchingEnum(Class<T> enumClass, String... values) {
        if (enumClass == null || values == null) {
            return null;
        }
        for (String value : values) {
            Optional<T> enumConstant = Enums.getIfPresent(enumClass, value.toUpperCase());
            if (enumConstant.isPresent()) {
                return enumConstant.get();
            }
        }
        return null; // Return null if no match is found
    }

    /**
     * This checks the stack trace for @Test to determine if a test is calling the code and skips.
     * @return true if it's a test.
     */
    public static boolean inTest() {
        return Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(e -> e.getClassName().endsWith("Test"));
    }

    /**
     * Strings old-style §-based color codes (used by ChatColor) from text
     * @param input text with color codes
     * @return  unformatted text
     */
    public static String stripColor(String input) {
        return input.replaceAll("(?i)§[0-9A-FK-ORX]", ""); // Use regex because it's fast and reliable
    }
    
}
