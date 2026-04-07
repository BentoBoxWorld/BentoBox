package world.bentobox.bentobox.util;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.nms.AbstractMetaData;
import world.bentobox.bentobox.nms.GetMetaData;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.nms.PasteHandlerImpl;
import world.bentobox.bentobox.nms.WorldRegenerator;
import world.bentobox.bentobox.nms.WorldRegeneratorImpl;


/**
 * A set of utility methods
 *
 * @author tastybento
 * @author Poslovitch
 */
public class Util {
    /**
     * The section sign character used for legacy color codes, replacing ChatColor.COLOR_CHAR.
     */
    private static final String COLOR_CHAR = "\u00A7";

    /**
     * Use standard color code definition: {@code &<hex>}.
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})");

    /**
     * Pattern to detect legacy color codes (ampersand or section sign followed by a color/format character).
     */
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("[&\u00A7][0-9a-fk-orA-FK-OR]");

    /**
     * Pattern to detect legacy hex color codes like {@code &#RRGGBB} or {@code §x§R§R...}.
     */
    private static final Pattern LEGACY_HEX_CODE_PATTERN = Pattern.compile("&#[0-9a-fA-F]{3,6}|\u00A7x(\u00A7[0-9a-fA-F]){6}");

    /**
     * MiniMessage instance for parsing MiniMessage-formatted strings.
     */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Serializer for converting Components to plain text (no formatting).
     */
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    /**
     * Map of legacy color code characters to their MiniMessage tag equivalents.
     */
    private static final java.util.Map<Character, String> LEGACY_TO_MM_MAP = java.util.Map.ofEntries(
            java.util.Map.entry('0', "black"),
            java.util.Map.entry('1', "dark_blue"),
            java.util.Map.entry('2', "dark_green"),
            java.util.Map.entry('3', "dark_aqua"),
            java.util.Map.entry('4', "dark_red"),
            java.util.Map.entry('5', "dark_purple"),
            java.util.Map.entry('6', "gold"),
            java.util.Map.entry('7', "gray"),
            java.util.Map.entry('8', "dark_gray"),
            java.util.Map.entry('9', "blue"),
            java.util.Map.entry('a', "green"),
            java.util.Map.entry('b', "aqua"),
            java.util.Map.entry('c', "red"),
            java.util.Map.entry('d', "light_purple"),
            java.util.Map.entry('e', "yellow"),
            java.util.Map.entry('f', "white"),
            java.util.Map.entry('k', "obfuscated"),
            java.util.Map.entry('l', "bold"),
            java.util.Map.entry('m', "strikethrough"),
            java.util.Map.entry('n', "underlined"),
            java.util.Map.entry('o', "italic"),
            java.util.Map.entry('r', "reset")
    );

    /**
     * Pattern to match inline command bracket syntax used in sendRawMessage.
     */
    private static final Pattern INLINE_CMD_PATTERN = Pattern.compile("\\[(run_command|suggest_command|copy_to_clipboard|open_url|hover): ([^\\]]+)]", Pattern.CASE_INSENSITIVE);

    private static final String NETHER = "_nether";
    private static final String THE_END = "_the_end";
    private static final String SNAPSHOT = "-SNAPSHOT";
    private static final String SERVER_VERSION = Bukkit.getMinecraftVersion();
    
    private static String serverVersion = null;
    private static BentoBox plugin = BentoBox.getInstance();
    private static PasteHandler pasteHandler = null;
    private static WorldRegenerator regenerator = null;

    private static GetMetaData metaData;

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
        if (s == null || s.trim().isEmpty()) {
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
        if (entity == null || entity.getType() == null) {
            return true;
        }
        // Check built-in class hierarchy for common passive mobs
        boolean isPassiveByClass = entity instanceof Animals
                || entity instanceof IronGolem 
                || entity instanceof Snowman 
                || entity instanceof Bat 
                || entity instanceof Allay;

        // Check WaterMob hierarchy, excluding PufferFish (hostile)
        boolean isPassiveWaterMob = entity instanceof WaterMob && !(entity instanceof PufferFish);

        // Check for newer entity types by their enum name (String comparison is safe across versions)
        boolean isCopperGolem = entity.getType().name().equals("COPPER_GOLEM");
        // And the sniffer
        boolean isSniffer = entity.getType().name().equals("SNIFFER");

        return isPassiveByClass || isPassiveWaterMob || isCopperGolem || isSniffer;
    }

    public static boolean isTamableEntity(Entity entity) {
        return entity instanceof Tameable tameable && tameable.isTamed();
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
        return teleportAsync(entity, location, TeleportCause.PLUGIN);
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
            // Method does not exist, fallback to default behavior
        } catch (Exception e) {
            BentoBox.getInstance().logStacktrace(e);
        }
        // Fallback
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
        String[] versionParts = version.replace(SNAPSHOT, "").split("\\.");
        String[] requiredVersionParts = requiredVersion.replace(SNAPSHOT, "").split("\\.");

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
        boolean isVersionSnapshot = version.contains(SNAPSHOT);
        boolean isRequiredSnapshot = requiredVersion.contains(SNAPSHOT);

        // If required version is a full release but current version is SNAPSHOT, it's incompatible
        return isRequiredSnapshot || !isVersionSnapshot;
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
     * Multi-line strings are processed line by line to ensure each line retains its own
     * color codes, since Adventure's LegacyComponentSerializer may omit repeated color
     * codes for consecutive segments of the same color.
     * @param textToColor Text which color codes must be parsed.
     * @return String text with parsed colors and stripped whitespaces after them.
     * @deprecated Use {@link #parseMiniMessageOrLegacy(String)} for Component output,
     *             or {@link #componentToLegacy(Component)} if a legacy string is needed.
     */
    @Deprecated(since = "3.2.0")
    @NonNull
    public static String translateColorCodes(@NonNull String textToColor) {
        // Process each line independently so color codes are not lost at line boundaries.
        // Adventure's LegacyComponentSerializer omits repeated §X codes when consecutive
        // components share the same color, causing lines 2+ to lose their color when split.
        if (textToColor.contains("\n")) {
            return Arrays.stream(textToColor.split("\n", -1))
                    .map(Util::translateColorCodes)
                    .collect(Collectors.joining("\n"));
        }
        // Use matcher to find hex patterns in given text.
        Matcher matcher = HEX_PATTERN.matcher(textToColor);
        // Increase buffer size by 32 like it is in bungee cord api. Use buffer because it is sync.
        StringBuilder buffer = new StringBuilder(textToColor.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);

            if (group.length() == 6) {
                // 6-digit hex: keep as &#RRGGBB for LEGACY_SERIALIZER to handle natively.
                matcher.appendReplacement(buffer, "&#" + group);
            } else {
                // 3-digit hex: expand to 6-digit &#RRGGBB format (e.g., &#fff -> &#ffffff).
                matcher.appendReplacement(buffer, "&#"
                        + group.charAt(0) + group.charAt(0)
                        + group.charAt(1) + group.charAt(1)
                        + group.charAt(2) + group.charAt(2));
            }
        }

        // Use Adventure's LegacyComponentSerializer to translate '&' color codes
        // then serialize back with section sign, and strip spaces after color codes.
        String withHexCodes = matcher.appendTail(buffer).toString();
        String translated = SECTION_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(withHexCodes));
        return Util.stripSpaceAfterColorCodes(translated);
    }


    /**
     * Strips spaces immediately after color codes. Used by {@link User#getTranslation(String, String...)}.
     * @param textToStrip - text to strip
     * @return text with spaces after color codes removed
     * @since 1.9.0
     * @deprecated No longer needed with MiniMessage format. Legacy locale files had spaces
     *             after color codes as a translation tool hack; MiniMessage tags don't need this.
     */
    @Deprecated(since = "3.2.0")
    @NonNull
    public static String stripSpaceAfterColorCodes(String textToStrip) {
        if (textToStrip == null) return "";
        textToStrip = textToStrip.replaceAll("(\u00A7.)[\\s]", "$1");
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

    /**
     * Get metadata decoder
     * @return an accelerated metadata class for this server
     */
    public static AbstractMetaData getMetaData() {
        if (metaData == null) {
            metaData = new GetMetaData();
        }
        return metaData;
    }

    /**
     * Get the regenerator the plugin will use
     * @return an accelerated regenerator class for this server
     */
    public static WorldRegenerator getRegenerator() {
        if (regenerator == null) {
            regenerator = new WorldRegeneratorImpl();
        }
        return regenerator;
    }

    /**
     * Checks what version the server is running and picks the appropriate NMS handler, or fallback
     * @return PasteHandler
     */
    public static PasteHandler getPasteHandler() {
        if (pasteHandler == null) {
            pasteHandler = new PasteHandlerImpl();
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
     */
    public static <T extends Enum<T>> T findFirstMatchingEnum(Class<T> enumClass, String... values) {
        if (enumClass == null || values == null) {
            return null;
        }
        for (String value : values) {
            Optional<T> enumConstant = Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.name().equals(value.toUpperCase())).findFirst();
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

    /**
     * Simple utility method to check if the server version is at least the target version.
     */
    public static boolean isVersionAtLeast(String targetVersion) {
        // Simple string comparison may be sufficient for minor versions, 
        // but a proper numeric check is safer for major releases.
        try {
            // Get major, minor, patch versions
            String[] currentParts = SERVER_VERSION.split("\\.");
            String[] targetParts = targetVersion.split("\\.");

            for (int i = 0; i < targetParts.length; i++) {
                int current = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
                int target = Integer.parseInt(targetParts[i]);

                if (current > target) return true;
                if (current < target) return false;
            }
            // All parts checked are equal (e.g., 1.21.9 vs 1.21.9)
            return true;
        } catch (NumberFormatException e) {
            // Fallback for non-standard version strings
            return SERVER_VERSION.startsWith(targetVersion);
        }
    }
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors() // Enables support for modern hex codes (e.g., &#FF0000) alongside legacy codes.
            .build();

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    
    /**
     * Converts a string containing Bukkit color codes ('&') into an Adventure Component.
     *
     * @param legacyString The string with Bukkit color and format codes.
     * @return The resulting Adventure Component.
     */
    public static Component bukkitToAdventure(String legacyString) {
        if (legacyString == null) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(legacyString);
    }

    // ---- MiniMessage support methods ----

    /**
     * Checks whether the given string contains legacy color codes ({@code &X} or {@code §X}).
     *
     * @param text the text to check
     * @return true if legacy color codes are detected
     * @since 3.2.0
     */
    public static boolean isLegacyFormat(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return LEGACY_CODE_PATTERN.matcher(text).find() || LEGACY_HEX_CODE_PATTERN.matcher(text).find();
    }

    /**
     * Converts a string containing legacy {@code &}/{@code §} color codes into MiniMessage format.
     * Strips spaces immediately after color codes (the legacy locale hack).
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code &c This is red} → {@code <red>This is red}</li>
     *   <li>{@code &l bold &r normal} → {@code <bold>bold </bold>normal}</li>
     *   <li>{@code &#FF0000 hex} → {@code <color:#FF0000>hex}</li>
     * </ul>
     *
     * @param legacy the legacy-formatted string
     * @return MiniMessage-formatted string
     * @since 3.2.0
     */
    @NonNull
    public static String legacyToMiniMessage(@NonNull String legacy) {
        if (legacy.isEmpty()) {
            return legacy;
        }
        // First, normalize § to & for uniform processing
        String text = legacy.replace('\u00A7', '&');

        // Convert hex codes &#RRGGBB → <color:#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            if (hex.length() == 3) {
                // Expand 3-digit to 6-digit
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            hexMatcher.appendReplacement(sb, "<color:#" + hex + ">");
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        // Convert &X codes to MiniMessage tags
        // We need to track open tags to properly close them
        StringBuilder result = new StringBuilder();
        List<String> openTags = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '&') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String mmTag = LEGACY_TO_MM_MAP.get(code);
                if (mmTag != null) {
                    i += 2;
                    // Strip space after color code (the locale hack)
                    if (i < text.length() && text.charAt(i) == ' ') {
                        i++;
                    }
                    if ("reset".equals(mmTag)) {
                        // Close all open tags
                        for (int j = openTags.size() - 1; j >= 0; j--) {
                            result.append("</").append(openTags.get(j)).append(">");
                        }
                        openTags.clear();
                    } else {
                        // Color codes reset previous colors but not decorations
                        // Decorations (bold, italic, etc.) are additive until reset
                        boolean isDecoration = mmTag.equals("bold") || mmTag.equals("italic")
                                || mmTag.equals("underlined") || mmTag.equals("strikethrough")
                                || mmTag.equals("obfuscated");
                        if (!isDecoration) {
                            // Close previous color tags (not decorations).
                            // To maintain proper MiniMessage nesting, close any decoration
                            // tags that sit inside color tags first, then reopen them after.
                            List<String> decorationsToReopen = new ArrayList<>();
                            for (int j = openTags.size() - 1; j >= 0; j--) {
                                String tag = openTags.get(j);
                                if (tag.equals("bold") || tag.equals("italic") || tag.equals("underlined")
                                        || tag.equals("strikethrough") || tag.equals("obfuscated")) {
                                    // Close decoration temporarily to maintain nesting
                                    result.append("</").append(tag).append(">");
                                    decorationsToReopen.add(0, tag);
                                    openTags.remove(j);
                                } else {
                                    // Named color or color:# tag — close it
                                    result.append("</").append(tag).append(">");
                                    openTags.remove(j);
                                }
                            }
                            // Reopen decorations so they continue through the new color
                            for (String dec : decorationsToReopen) {
                                result.append("<").append(dec).append(">");
                                openTags.add(dec);
                            }
                        }
                        result.append("<").append(mmTag).append(">");
                        openTags.add(mmTag);
                    }
                    continue;
                }
            }
            // Handle <color:#RRGGBB> that we already inserted
            if (text.charAt(i) == '<' && text.substring(i).startsWith("<color:#")) {
                int end = text.indexOf('>', i);
                if (end != -1) {
                    String colorTag = text.substring(i + 1, end);
                    // Close previous color tags, preserving decoration nesting
                    List<String> decorationsToReopen = new ArrayList<>();
                    for (int j = openTags.size() - 1; j >= 0; j--) {
                        String tag = openTags.get(j);
                        if (tag.equals("bold") || tag.equals("italic") || tag.equals("underlined")
                                || tag.equals("strikethrough") || tag.equals("obfuscated")) {
                            // Close decoration temporarily to maintain nesting
                            result.append("</").append(tag).append(">");
                            decorationsToReopen.add(0, tag);
                            openTags.remove(j);
                        } else {
                            result.append("</").append(tag).append(">");
                            openTags.remove(j);
                        }
                    }
                    // Reopen decorations so they continue through the new color
                    for (String dec : decorationsToReopen) {
                        result.append("<").append(dec).append(">");
                        openTags.add(dec);
                    }
                    result.append("<").append(colorTag).append(">");
                    openTags.add(colorTag);
                    i = end + 1;
                    // Strip space after hex color code
                    if (i < text.length() && text.charAt(i) == ' ') {
                        i++;
                    }
                    continue;
                }
            }
            result.append(text.charAt(i));
            i++;
        }
        // Close any remaining open tags
        for (int j = openTags.size() - 1; j >= 0; j--) {
            String tag = openTags.get(j);
            int parameterSeparator = tag.indexOf(':');
            String closingTag = parameterSeparator >= 0 ? tag.substring(0, parameterSeparator) : tag;
            result.append("</").append(closingTag).append(">");
        }

        return result.toString();
    }

    /**
     * Parses a MiniMessage-formatted string into an Adventure Component.
     *
     * @param miniMessageString the MiniMessage string
     * @return parsed Component
     * @since 3.2.0
     */
    /**
     * Replaces legacy {@code &X} and {@code §X} color/format codes with MiniMessage opening tags inline,
     * without adding closing tags. This is safe for strings that already contain MiniMessage tags
     * (mixed content), because MiniMessage handles unclosed tags correctly — they apply until
     * overridden by another tag or the end of the string.
     * <p>
     * Unlike {@link #legacyToMiniMessage(String)}, this method does not track or emit closing tags,
     * avoiding nesting issues when existing MiniMessage closing tags are present.
     *
     * @param text the string with mixed legacy codes and MiniMessage tags
     * @return string with legacy codes replaced by MiniMessage opening tags
     * @since 3.2.0
     */
    @NonNull
    public static String replaceLegacyCodesInline(@NonNull String text) {
        // Normalize § to &
        text = text.replace('\u00A7', '&');
        // Replace hex codes &#RRGGBB → <color:#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            hexMatcher.appendReplacement(sb, "<color:#" + hex + ">");
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();
        // Replace &X codes with MiniMessage tags (opening only, no closing)
        sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '&') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String mmTag = LEGACY_TO_MM_MAP.get(code);
                if (mmTag != null) {
                    sb.append("<").append(mmTag).append(">");
                    i += 2;
                    // Strip space after color code (locale hack)
                    if (i < text.length() && text.charAt(i) == ' ') {
                        i++;
                    }
                    continue;
                }
            }
            sb.append(text.charAt(i));
            i++;
        }
        return sb.toString();
    }

    @NonNull
    public static Component parseMiniMessage(@NonNull String miniMessageString) {
        return MINI_MESSAGE.deserialize(miniMessageString);
    }

    /**
     * Parses a MiniMessage-formatted string with tag resolvers into an Adventure Component.
     *
     * @param miniMessageString the MiniMessage string
     * @param resolvers tag resolvers for placeholder substitution
     * @return parsed Component
     * @since 3.2.0
     */
    @NonNull
    public static Component parseMiniMessage(@NonNull String miniMessageString, @NonNull TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(miniMessageString, resolvers);
    }

    /**
     * Auto-detects whether the string uses legacy color codes or MiniMessage format,
     * and parses it into an Adventure Component accordingly.
     *
     * @param text the text (either legacy or MiniMessage format)
     * @return parsed Component
     * @since 3.2.0
     */
    @NonNull
    public static Component parseMiniMessageOrLegacy(@NonNull String text) {
        if (isLegacyFormat(text)) {
            boolean hasMiniMessage = text.contains("<") && text.contains(">");
            if (hasMiniMessage) {
                // Mixed content: use inline replacement to avoid nesting issues
                return MINI_MESSAGE.deserialize(replaceLegacyCodesInline(text));
            }
            return MINI_MESSAGE.deserialize(legacyToMiniMessage(text));
        }
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Converts inline command bracket syntax to MiniMessage click/hover tags.
     * <p>
     * Converts:
     * <ul>
     *   <li>{@code [run_command: /cmd]} → {@code <click:run_command:/cmd>}</li>
     *   <li>{@code [suggest_command: /cmd]} → {@code <click:suggest_command:/cmd>}</li>
     *   <li>{@code [copy_to_clipboard: text]} → {@code <click:copy_to_clipboard:text>}</li>
     *   <li>{@code [open_url: url]} → {@code <click:open_url:url>}</li>
     *   <li>{@code [hover: text]} → {@code <hover:show_text:'text'>}</li>
     * </ul>
     * The click and hover tags wrap the entire remaining message content.
     *
     * @param message the message with bracket-syntax inline commands
     * @return message with MiniMessage click/hover tags
     * @since 3.2.0
     */
    @NonNull
    public static String convertInlineCommandsToMiniMessage(@NonNull String message) {
        // Handle escaped double brackets first: [[text]] → text (literal)
        message = message.replace("[[", "\u0000LBRACKET\u0000").replace("]]", "\u0000RBRACKET\u0000");

        // Extract all inline commands
        Matcher matcher = INLINE_CMD_PATTERN.matcher(message);
        String clickTag = null;
        String hoverTag = null;

        // Collect commands and strip them from the text
        StringBuilder cleanText = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            cleanText.append(message, lastEnd, matcher.start());
            String action = matcher.group(1).toLowerCase(java.util.Locale.ENGLISH);
            String value = matcher.group(2);
            switch (action) {
                case "hover" -> {
                    if (hoverTag == null) {
                        // Escape single quotes in hover text
                        hoverTag = "<hover:show_text:'" + value.replace("'", "\\'") + "'>";
                    }
                }
                case "run_command", "suggest_command", "copy_to_clipboard", "open_url" -> {
                    if (clickTag == null) {
                        clickTag = "<click:" + action + ":" + value + ">";
                    }
                }
                default -> cleanText.append(matcher.group()); // unknown, keep as-is
            }
            lastEnd = matcher.end();
        }
        cleanText.append(message.substring(lastEnd));
        String result = cleanText.toString();

        // Restore escaped brackets
        result = result.replace("\u0000LBRACKET\u0000", "[").replace("\u0000RBRACKET\u0000", "]");

        // Wrap the text with click and hover tags
        if (clickTag != null || hoverTag != null) {
            String prefix = (hoverTag != null ? hoverTag : "") + (clickTag != null ? clickTag : "");
            String suffix = (clickTag != null ? "</click>" : "") + (hoverTag != null ? "</hover>" : "");
            result = prefix + result + suffix;
        }

        return result;
    }

    /**
     * Serializes an Adventure Component back to a legacy §-coded string.
     * Useful for backwards compatibility with APIs that still expect legacy strings.
     *
     * @param component the Adventure Component
     * @return legacy-formatted string with § color codes
     * @since 3.2.0
     */
    @NonNull
    public static String componentToLegacy(@NonNull Component component) {
        StringBuilder sb = new StringBuilder();
        // EmittedState[0] holds the last-emitted style (color + decorations) so the walker
        // can compute transitions and emit §r where Adventure's serializer would not.
        EmittedState state = new EmittedState();
        appendComponentLegacy(sb, component, Style.empty(), state);
        return sb.toString();
    }

    /**
     * Mutable state used by {@link #appendComponentLegacy(StringBuilder, Component, Style, EmittedState)}
     * to track the most recently emitted color and decorations. Adventure's
     * {@link LegacyComponentSerializer} silently drops decoration-off transitions because legacy
     * color codes have no "turn this decoration off" code — only §r resets everything. We work
     * around that by tracking what is currently active and emitting §r ourselves when needed.
     */
    private static final class EmittedState {
        TextColor color;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;
        boolean isFresh = true;
    }

    private static void appendComponentLegacy(StringBuilder sb, Component component, Style inherited, EmittedState state) {
        // merge() with default strategy lets the child component override inherited fields,
        // and inherits the parent's fields where the child leaves them unset.
        Style effective = inherited.merge(component.style());
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            emitStyleTransition(sb, effective, state);
            sb.append(text.content());
        }
        for (Component child : component.children()) {
            appendComponentLegacy(sb, child, effective, state);
        }
    }

    private static void emitStyleTransition(StringBuilder sb, Style style, EmittedState state) {
        boolean wantBold = style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE;
        boolean wantItalic = style.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE;
        boolean wantUnderlined = style.decoration(TextDecoration.UNDERLINED) == TextDecoration.State.TRUE;
        boolean wantStrikethrough = style.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE;
        boolean wantObfuscated = style.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE;
        TextColor wantColor = style.color();

        // Determine if we need a hard reset: any decoration that was on must turn off,
        // or the color must change to "no color" while one was previously active.
        boolean needReset = (state.bold && !wantBold)
                || (state.italic && !wantItalic)
                || (state.underlined && !wantUnderlined)
                || (state.strikethrough && !wantStrikethrough)
                || (state.obfuscated && !wantObfuscated)
                || (state.color != null && wantColor == null);

        if (needReset) {
            sb.append(COLOR_CHAR).append('r');
            state.color = null;
            state.bold = false;
            state.italic = false;
            state.underlined = false;
            state.strikethrough = false;
            state.obfuscated = false;
        }

        // Emit color if it changed (or after a reset)
        if (wantColor != null && (state.isFresh || !wantColor.equals(state.color) || needReset)) {
            sb.append(legacyColorCode(wantColor));
            state.color = wantColor;
        }

        // Emit decorations that should now be on but aren't yet
        if (wantBold && !state.bold) {
            sb.append(COLOR_CHAR).append('l');
            state.bold = true;
        }
        if (wantItalic && !state.italic) {
            sb.append(COLOR_CHAR).append('o');
            state.italic = true;
        }
        if (wantUnderlined && !state.underlined) {
            sb.append(COLOR_CHAR).append('n');
            state.underlined = true;
        }
        if (wantStrikethrough && !state.strikethrough) {
            sb.append(COLOR_CHAR).append('m');
            state.strikethrough = true;
        }
        if (wantObfuscated && !state.obfuscated) {
            sb.append(COLOR_CHAR).append('k');
            state.obfuscated = true;
        }
        state.isFresh = false;
    }

    private static String legacyColorCode(TextColor color) {
        // For named colors, use the standard single-character legacy code.
        NamedTextColor named = NamedTextColor.nearestTo(color);
        char code;
        if (named == NamedTextColor.BLACK) code = '0';
        else if (named == NamedTextColor.DARK_BLUE) code = '1';
        else if (named == NamedTextColor.DARK_GREEN) code = '2';
        else if (named == NamedTextColor.DARK_AQUA) code = '3';
        else if (named == NamedTextColor.DARK_RED) code = '4';
        else if (named == NamedTextColor.DARK_PURPLE) code = '5';
        else if (named == NamedTextColor.GOLD) code = '6';
        else if (named == NamedTextColor.GRAY) code = '7';
        else if (named == NamedTextColor.DARK_GRAY) code = '8';
        else if (named == NamedTextColor.BLUE) code = '9';
        else if (named == NamedTextColor.GREEN) code = 'a';
        else if (named == NamedTextColor.AQUA) code = 'b';
        else if (named == NamedTextColor.RED) code = 'c';
        else if (named == NamedTextColor.LIGHT_PURPLE) code = 'd';
        else if (named == NamedTextColor.YELLOW) code = 'e';
        else code = 'f';
        // If the original color was a true hex (not a named color), emit the §x§R§R... form
        // so it round-trips. Otherwise just emit the named code.
        if (!(color instanceof NamedTextColor)) {
            String hex = String.format("%06X", color.value());
            StringBuilder out = new StringBuilder();
            out.append(COLOR_CHAR).append('x');
            for (int i = 0; i < 6; i++) {
                out.append(COLOR_CHAR).append(hex.charAt(i));
            }
            return out.toString();
        }
        return COLOR_CHAR + Character.toString(code);
    }

    /**
     * Serializes an Adventure Component to plain text with no formatting.
     *
     * @param component the Adventure Component
     * @return plain text string
     * @since 3.2.0
     */
    @NonNull
    public static String componentToPlainText(@NonNull Component component) {
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Returns the shared MiniMessage instance.
     *
     * @return MiniMessage instance
     * @since 3.2.0
     */
    @NonNull
    public static MiniMessage getMiniMessage() {
        return MINI_MESSAGE;
    }
}
