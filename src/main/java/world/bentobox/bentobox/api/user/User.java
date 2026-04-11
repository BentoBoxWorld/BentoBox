package world.bentobox.bentobox.api.user;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Vibration;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.OfflineMessageEvent;
import world.bentobox.bentobox.api.metadata.MetaDataAble;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.util.Util;

/**
 * Combines {@link Player}, {@link OfflinePlayer} and {@link CommandSender} to
 * provide convenience methods related to localization and generic interactions.
 * <br/>
 * Therefore, a User could usually be a Player, an OfflinePlayer or the server's
 * console. Preliminary checks should be performed before trying to run methods
 * that relies on a specific implementation. <br/>
 * <br/>
 * It is good practice to use the User instance whenever possible instead of
 * Player or CommandSender.
 *
 * @author tastybento
 */
public class User implements MetaDataAble {

    private static final Map<UUID, User> users = new HashMap<>();

    // Patterns for message delivery type tags in locale strings
    private static final Pattern ACTIONBAR_PATTERN = Pattern.compile("^\\[actionbar]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("^\\[title]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBTITLE_PATTERN = Pattern.compile("\\[subtitle]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOUND_PATTERN = Pattern.compile("\\[sound:([^:\\]]+)(?::([^:\\]]+))?(?::([^:\\]]+))?]", Pattern.CASE_INSENSITIVE);

    // Used for particle validation
    private static final Map<Particle, Class<?>> VALIDATION_CHECK;
    static {
        Map<Particle, Class<?>> v = new EnumMap<>(Particle.class);
        v.put(Enums.getIfPresent(Particle.class, "DUST")
                .or(Enums.getIfPresent(Particle.class, "REDSTONE").or(Particle.FLAME)), Particle.DustOptions.class);
        if (Enums.getIfPresent(Particle.class, "ITEM").isPresent()) {
            // 1.20.6 Particles
            v.put(Particle.ITEM, ItemStack.class);
            v.put(Particle.ITEM_COBWEB, ItemStack.class);
            v.put(Particle.BLOCK, BlockData.class);
            v.put(Particle.DUST_PILLAR, BlockData.class);
            v.put(Particle.ENTITY_EFFECT, Color.class);
        }
        v.put(Particle.FALLING_DUST, BlockData.class);
        v.put(Particle.BLOCK_MARKER, BlockData.class);
        v.put(Particle.DUST_COLOR_TRANSITION, DustTransition.class);
        v.put(Particle.VIBRATION, Vibration.class);
        v.put(Particle.SCULK_CHARGE, Float.class);
        v.put(Particle.SHRIEK, Integer.class);

        VALIDATION_CHECK = Collections.unmodifiableMap(v);
    }

    /**
     * Clears all users from the user list
     */
    public static void clearUsers() {
        users.clear();
    }

    /**
     * Gets an instance of User from a CommandSender
     * 
     * @param sender - command sender, e.g. console
     * @return user - user
     */
    @NonNull
    public static User getInstance(@NonNull CommandSender sender) {
        if (sender instanceof Player p) {
            return getInstance(p);
        }
        // Console
        return new User(sender);
    }

    /**
     * Gets an instance of User from a Player object.
     * 
     * @param player - the player
     * @return user - user
     */
    @NonNull
    public static User getInstance(@NonNull Player player) {
        if (users.containsKey(player.getUniqueId())) {
            return users.get(player.getUniqueId());
        }
        return new User(player);
    }

    /**
     * Gets an instance of User from a UUID. This will always return a user object.
     * If the player is offline then the getPlayer value will be null.
     * 
     * @param uuid - UUID
     * @return user - user
     */
    @NonNull
    public static User getInstance(@NonNull UUID uuid) {
        if (users.containsKey(uuid)) {
            return users.get(uuid);
        }
        // Return a user instance
        return new User(uuid);
    }

    /**
     * Gets an instance of User from an OfflinePlayer
     * 
     * @param offlinePlayer offline Player
     * @return user
     * @since 1.3.0
     */
    @NonNull
    public static User getInstance(@NonNull OfflinePlayer offlinePlayer) {
        if (users.containsKey(offlinePlayer.getUniqueId())) {
            return users.get(offlinePlayer.getUniqueId());
        }
        return new User(offlinePlayer);
    }

    /**
     * Removes this player from the User cache and player manager cache
     * 
     * @param player the player
     */
    public static void removePlayer(Player player) {
        if (player != null) {
            users.remove(player.getUniqueId());
        }
    }

    // ----------------------------------------------------

    private static BentoBox plugin = BentoBox.getInstance();

    @Nullable
    private final Player player;
    private OfflinePlayer offlinePlayer;
    private final UUID playerUUID;
    @Nullable
    private final CommandSender sender;

    private Addon addon;

    private User(@Nullable CommandSender sender) {
        player = null;
        playerUUID = null;
        this.sender = sender;
    }

    private User(@NonNull Player player) {
        this.player = player;
        offlinePlayer = player;
        sender = player;
        playerUUID = player.getUniqueId();
        users.put(playerUUID, this);
    }

    private User(@NonNull OfflinePlayer offlinePlayer) {
        this.player = offlinePlayer.isOnline() ? offlinePlayer.getPlayer() : null;
        this.playerUUID = offlinePlayer.getUniqueId();
        this.sender = offlinePlayer.isOnline() ? offlinePlayer.getPlayer() : null;
        this.offlinePlayer = offlinePlayer;
    }

    private User(UUID playerUUID) {
        player = Bukkit.getPlayer(playerUUID);
        this.playerUUID = playerUUID;
        sender = player;
        offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
    }

    /**
     * Used for testing
     * 
     * @param p - plugin
     */
    public static void setPlugin(BentoBox p) {
        plugin = p;
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return sender == null ? Set.of() : sender.getEffectivePermissions();
    }

    /**
     * Get the user's inventory
     * 
     * @return player's inventory
     */
    @NonNull
    public PlayerInventory getInventory() {
        return Objects.requireNonNull(player, "getInventory can only be called for online players!").getInventory();
    }

    /**
     * Get the user's location
     * 
     * @return location
     */
    @NonNull
    public Location getLocation() {
        Player p = Objects.requireNonNull(player, "getLocation can only be called for online players!");
        Location loc = p.getLocation();
        return Objects.requireNonNull(loc, "Player's location cannot be null! The player may not have a location (has never played).");
    }

    /**
     * Get the user's name
     * 
     * @return player's name
     */
    @NonNull
    public String getName() {
        return player != null ? player.getName() : plugin.getPlayers().getName(playerUUID);
    }

    /**
     * Get the user's display name
     * 
     * @return player's display name if the player is online otherwise just their
     *         name
     * @since 1.22.1
     */
    @SuppressWarnings("deprecation")
    @NonNull
    public String getDisplayName() {
        return player != null ? player.getDisplayName() : plugin.getPlayers().getName(playerUUID);
    }

    /**
     * Get the user's display name as a text Component
     * 
     * @return player's display name if the player is online otherwise just their
     *         name
     * @since 3.4.0
     */
    public Component displayName() {
        return player != null ? player.displayName() : Component.text(plugin.getPlayers().getName(playerUUID));
    }

    /**
     * Check if the User is a player before calling this method. {@link #isPlayer()}
     * 
     * @return the player
     */
    @NonNull
    public Player getPlayer() {
        return Objects.requireNonNull(player, "User is not a player!");
    }

    /**
     * @return true if this user is a player, false if not, e.g., console
     */
    public boolean isPlayer() {
        return player != null;
    }

    /**
     * Use {@link #isOfflinePlayer()} before calling this method
     * 
     * @return the offline player
     * @since 1.3.0
     */
    @NonNull
    public OfflinePlayer getOfflinePlayer() {
        return Objects.requireNonNull(offlinePlayer, "User is not an OfflinePlayer!");
    }

    /**
     * @return true if this user is an OfflinePlayer, false if not, e.g., console
     * @since 1.3.0
     */
    public boolean isOfflinePlayer() {
        return offlinePlayer != null;
    }

    @Nullable
    public CommandSender getSender() {
        return sender;
    }

    public UUID getUniqueId() {
        return playerUUID;
    }

    /**
     * @param permission permission string
     * @return true if permission is empty or null or if the player has that
     *         permission or if the player is op.
     */
    public boolean hasPermission(@Nullable String permission) {
        return permission == null || permission.isEmpty() || isOp() || (sender != null && sender.hasPermission(permission));
    }

    /**
     * Removes permission from user
     * 
     * @param name - Name of the permission to remove
     * @return true if successful
     * @since 1.5.0
     */
    public boolean removePerm(String name) {
        if (player == null) {
            return false;
        }
        for (PermissionAttachmentInfo p : player.getEffectivePermissions()) {
            if (p.getPermission().equals(name) && p.getAttachment() != null) {
                player.removeAttachment(p.getAttachment());
                break;
            }
        }
        player.recalculatePermissions();
        return !player.hasPermission(name);
    }

    /**
     * Add a permission to user
     * 
     * @param name - Name of the permission to attach
     * @return The PermissionAttachment that was just created
     * @since 1.5.0
     */
    public PermissionAttachment addPerm(String name) {
        assert player != null;
        return player.addAttachment(plugin, name, true);
    }

    public boolean isOnline() {
        return player != null && player.isOnline();
    }

    /**
     * Checks if user is Op
     * 
     * @return true if user is Op
     */
    public boolean isOp() {
        if (sender != null) {
            return sender.isOp();
        }
        if (playerUUID != null && offlinePlayer != null) {
            return offlinePlayer.isOp();
        }
        return false;
    }

    /**
     * Get the maximum value of a numerical permission setting. If a player is given
     * an explicit negative number then this is treated as "unlimited" and returned
     * immediately.
     * 
     * @param permissionPrefix the start of the perm, e.g.,
     *                         {@code plugin.mypermission}
     * @param defaultValue     the default value; the result may be higher or lower
     *                         than this
     * @return max value
     */
    public int getPermissionValue(String permissionPrefix, int defaultValue) {
        // If requester is console, then return the default value
        if (!isPlayer() || player == null)
            return defaultValue;

        // If there is a dot at the end of the permissionPrefix, remove it
        if (permissionPrefix.endsWith(".")) {
            permissionPrefix = permissionPrefix.substring(0, permissionPrefix.length() - 1);
        }

        final String permPrefix = permissionPrefix + ".";

        List<String> permissions = player.getEffectivePermissions().stream().filter(PermissionAttachmentInfo::getValue) // Must
                // be
                // a
                // positive
                // permission,
                // not
                // a
                // negative
                // one
                .map(PermissionAttachmentInfo::getPermission).filter(permission -> permission.startsWith(permPrefix))
                .toList();

        if (permissions.isEmpty())
            return defaultValue;

        return iteratePerms(permissions, permPrefix, defaultValue);

    }

    private int iteratePerms(List<String> permissions, String permPrefix, int defaultValue) {
        int value = 0;
        if (player == null) {
            return 0;
        }
        for (String permission : permissions) {
            if (permission.contains(permPrefix + "*")) {
                // 'Star' permission
                return defaultValue;
            } else {
                String[] spl = permission.split(permPrefix);
                if (spl.length > 1) {
                    if (!NumberUtils.isCreatable(spl[1])) {
                        plugin.logError("Player " + player.getName() + " has permission: '" + permission);
                    } else {
                        int v = Integer.parseInt(spl[1]);
                        if (v < 0) {
                            return v;
                        }
                        value = Math.max(value, v);
                    }
                }
            }
        }

        return value;
    }

    /**
     * Gets a translation for a specific world
     * 
     * @param world     - world of translation
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go
     *                  in pairs, for example "[name]", "tastybento"
     * @return Translated string with colors converted, or the reference if nothing
     *         has been found
     * @since 1.3.0
     */
    public String getTranslation(World world, String reference, String... variables) {
        // Get translation.
        String addonPrefix = plugin.getIWM().getAddon(world)
                .map(a -> a.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".").orElse("");
        String raw = translate(addonPrefix, reference, variables);
        return convertToLegacy(raw);
    }

    /**
     * Gets a translation of this reference for this user with colors converted.
     * Translations may be overridden by Addons by using the same reference prefixed
     * by the addon name (from the Addon Description) in lower case.
     * 
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go
     *                  in pairs, for example "[name]", "tastybento"
     * @return Translated string with colors converted, or the reference if nothing
     *         has been found
     */
    public String getTranslation(String reference, String... variables) {
        // Get addonPrefix
        String addonPrefix = addon == null ? "" : addon.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
        String raw = translate(addonPrefix, reference, variables);
        return convertToLegacy(raw);
    }

    /**
     * Converts a raw translation string (which may contain MiniMessage tags, legacy &amp;/§ codes,
     * or a mix of both) into a legacy §-coded string for backwards compatibility.
     * <p>
     * Mixed content occurs when MiniMessage locale strings have variables substituted with
     * legacy-coded values from addons (e.g., {@code <bold>&ePlayer Name</bold>}).
     *
     * @param raw the raw translated string
     * @return legacy §-coded string
     */
    private String convertToLegacy(String raw) {
        boolean hasLegacy = Util.isLegacyFormat(raw);
        boolean hasMiniMessage = raw.contains("<") && raw.contains(">");
        if (hasLegacy && !hasMiniMessage) {
            // Pure legacy — use the old path
            @SuppressWarnings("deprecation")
            String result = Util.translateColorCodes(raw);
            return result;
        }
        // Process the whole string at once (not per line). MiniMessage tags can span
        // newlines — splitting first would orphan close tags (e.g. <green>foo\nbar</green>
        // becomes "<green>foo" and "bar</green>", and "</green>" with no opening would
        // be rendered as literal text). Component text preserves newlines through
        // serialization, so a single parse is correct.
        if (hasLegacy) {
            // Mixed content: MiniMessage tags + legacy & codes.
            // Replace legacy codes with MiniMessage opening tags inline (no closing tags).
            // MiniMessage handles unclosed tags correctly — they apply until overridden.
            // Using legacyToMiniMessage() would produce wrong nesting (e.g.,
            // <bold><yellow>text</bold></yellow> where </yellow> leaks as literal text).
            raw = Util.replaceLegacyCodesInline(raw);
        }
        // Parse as MiniMessage and serialize to legacy
        return Util.componentToLegacy(Util.parseMiniMessage(raw));
    }

    /**
     * Gets a translation of this reference for this user without colors translated.
     * Translations may be overridden by Addons by using the same reference prefixed
     * by the addon name (from the Addon Description) in lower case.
     * 
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go
     *                  in pairs, for example "[name]", "tastybento"
     * @return Translated string or the reference if nothing has been found
     * @since 1.17.4
     */
    public String getTranslationNoColor(String reference, String... variables) {
        // Get addonPrefix
        String addonPrefix = addon == null ? "" : addon.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
        return translate(addonPrefix, reference, variables);
    }

    private String translate(String addonPrefix, String reference, String[] variables) {
        // Try to get the translation for this specific addon
        String translation = plugin.getLocalesManager().get(this, addonPrefix + reference);

        if (translation == null) {
            // No luck, try to get the generic translation
            translation = plugin.getLocalesManager().get(this, reference);
            if (translation == null) {
                // Nothing found. Replace vars (probably will do nothing) and return
                return replaceVars(reference, variables);
            }
        }

        // If this is a prefix, just gather and return the translation
        if (!reference.startsWith("prefixes.")) {
            // Replace the prefixes
            return replacePrefixes(translation, variables);
        }
        return translation;
    }

    private String replacePrefixes(String translation, String[] variables) {
        for (String prefix : plugin.getLocalesManager().getAvailablePrefixes(this)) {
            String prefixTranslation = getTranslation("prefixes." + prefix);

            // Append a formatting reset so prefix decorations (bold, italic, etc.)
            // don't leak into the surrounding message text.
            if (Util.isLegacyFormat(prefixTranslation)) {
                prefixTranslation += "\u00A7r";
            }

            // Replace the prefix in the actual message
            translation = translation.replace("[prefix_" + prefix + "]", prefixTranslation);
        }

        // Then replace Placeholders, this will only work if this is a player
        if (player != null) {
            translation = plugin.getPlaceholdersManager().replacePlaceholders(player, translation);
        }

        // Then replace variables
        if (variables.length > 1) {
            for (int i = 0; i < variables.length; i += 2) {
                // Prevent a NPE if the substituting variable is null
                if (variables[i + 1] != null) {
                    translation = translation.replace(variables[i], variables[i + 1]);
                }
            }
        }

        // Replace game mode and friendly name in general
        // Replace the [gamemode] text variable
        if (addon != null && addon.getDescription() != null) {
            translation = translation.replace("[gamemode]", addon.getDescription().getName());
        }
        if (player != null) {
            // Replace the [friendly_name] text variable
            translation = translation.replace("[friendly_name]",
                    isPlayer() ? plugin.getIWM().getFriendlyName(getWorld()) : "[friendly_name]");
        }
        return translation;
    }

    private String replaceVars(String reference, String[] variables) {
        // Replace Placeholders, this will only work if this is a player
        if (player != null) {
            reference = plugin.getPlaceholdersManager().replacePlaceholders(player, reference);
        }
        // Validate variables array length
        if (variables.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Variable replacements must be in pairs (key, value), but got odd number: " + variables.length);
        }
        // Then replace variables
        if (variables.length > 1) {
            for (int i = 0; i < variables.length; i += 2) {
                reference = reference.replace(variables[i], variables[i + 1]);
            }
        }

        // If no translation has been found, return the reference for debug purposes.
        return reference;
    }

    /**
     * Gets a translation of this reference for this user.
     * 
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go
     *                  in pairs, for example "[name]", "tastybento"
     * @return Translated string with colors converted, or a blank String if nothing
     *         has been found
     */
    public String getTranslationOrNothing(String reference, String... variables) {
        String translation = getTranslation(reference, variables);
        return translation.equals(reference) ? "" : translation;
    }

    /**
     * Send a message to sender if message is not empty.
     * 
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     */
    public void sendMessage(String reference, String... variables) {
        String message = getTranslation(reference, variables);
        String noColors = message.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
        if (!noColors.isEmpty()) {
            sendRawMessage(message);
        }
    }

    /**
     * Sends a raw message to the sender, parsing inline commands embedded within square brackets.
     * <p>
     * The method supports embedding clickable and hoverable actions into the message text using inline commands.
     * Recognized commands are:
     * <ul>
     *   <li><code>[run_command: &lt;command&gt;]</code> - Runs the specified command when the message is clicked.</li>
     *   <li><code>[suggest_command: &lt;command&gt;]</code> - Suggests the specified command in the chat input.</li>
     *   <li><code>[copy_to_clipboard: &lt;text&gt;]</code> - Copies the specified text to the player's clipboard.</li>
     *   <li><code>[open_url: &lt;url&gt;]</code> - Opens the specified URL when the message is clicked.</li>
     *   <li><code>[hover: &lt;text&gt;]</code> - Shows the specified text when the message is hovered over.</li>
     * </ul>
     * <p>
     * The commands can be placed anywhere in the message and will apply to the entire message component.
     * If multiple commands of the same type are provided, only the first one encountered will be applied.
     * Unrecognized or invalid commands enclosed in square brackets will be preserved in the output text.
     * <p>
     * Example usage:
     * <pre>
     * sendRawMessage("Hello [not-a-command: hello][run_command: /help] World [hover: This is a hover text]");
     * </pre>
     * The above message will display "Hello [not-a-command: hello] World" where clicking the message runs the "/help" command,
     * and hovering over the message shows "This is a hover text".
     * <p>
     * Additionally, the method supports message delivery type tags:
     * <ul>
     *   <li><code>[actionbar]</code> - Sends the message as an action bar message.</li>
     *   <li><code>[title]</code> - Sends the message as a title. Optionally use <code>[subtitle]</code> to split title and subtitle.</li>
     *   <li><code>[sound:name:volume:pitch]</code> - Plays the specified sound to the player. Volume and pitch are optional (default 1.0).</li>
     * </ul>
     *
     * @param message The message to send, containing inline commands in square brackets.
     */
    public void sendRawMessage(String message) {
        if (sender == null) {
            Bukkit.getPluginManager().callEvent(new OfflineMessageEvent(this.playerUUID, message));
            return;
        }

        // Extract and play sounds first (can combine with any delivery type)
        String remaining = processAndStripSounds(message);

        // Determine delivery type and dispatch
        if (ACTIONBAR_PATTERN.matcher(remaining).find()) {
            String text = ACTIONBAR_PATTERN.matcher(remaining).replaceFirst("");
            Component component = parseToComponent(text);
            if (sender instanceof Player player) {
                player.sendActionBar(component);
            } else {
                sender.sendMessage(component);
            }
        } else if (TITLE_PATTERN.matcher(remaining).find()) {
            String text = TITLE_PATTERN.matcher(remaining).replaceFirst("");
            Component titleComponent;
            Component subtitleComponent = Component.empty();
            Matcher subtitleMatcher = SUBTITLE_PATTERN.matcher(text);
            if (subtitleMatcher.find()) {
                String titleText = text.substring(0, subtitleMatcher.start());
                String subtitleText = text.substring(subtitleMatcher.end());
                titleComponent = parseToComponent(titleText);
                subtitleComponent = parseToComponent(subtitleText);
            } else {
                titleComponent = parseToComponent(text);
            }
            if (sender instanceof Player player) {
                player.showTitle(Title.title(titleComponent, subtitleComponent));
            } else {
                sender.sendMessage(titleComponent);
            }
        } else {
            // Default: chat message
            Component component = parseToComponent(remaining);
            sender.sendMessage(component);
        }
    }

    /**
     * Parses a message string into an Adventure Component, handling inline commands and
     * auto-detecting legacy or MiniMessage format.
     *
     * @param text the message text
     * @return the parsed Component
     */
    private Component parseToComponent(String text) {
        String mmMessage = Util.convertInlineCommandsToMiniMessage(text);
        return Util.parseMiniMessageOrLegacy(mmMessage);
    }

    /**
     * Extracts sound tags from the message, plays them for the player, and returns
     * the message with sound tags stripped out. Sound names use underscores in locale
     * files for readability (e.g., {@code entity_experience_orb_pickup}), which are
     * converted to Minecraft's dot-separated resource location format
     * (e.g., {@code entity.experience.orb.pickup}).
     *
     * @param message the message possibly containing [sound:name:volume:pitch] tags
     * @return the message with sound tags removed
     */
    private String processAndStripSounds(String message) {
        Matcher matcher = SOUND_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String soundName = matcher.group(1).toLowerCase(Locale.ENGLISH).replace('_', '.');
            float volume = parseFloatOrDefault(matcher.group(2), 1.0f);
            float pitch = parseFloatOrDefault(matcher.group(3), 1.0f);
            if (sender instanceof Player player) {
                player.playSound(player.getLocation(), soundName, volume, pitch);
            }
            matcher.appendReplacement(sb, "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Parses a string to a float, returning a default value if the string is null
     * or not a valid float.
     *
     * @param value the string to parse, may be null
     * @param defaultValue the default value if parsing fails
     * @return the parsed float or the default value
     */
    private static float parseFloatOrDefault(@Nullable String value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * Sends an Adventure Component directly to the user.
     *
     * @param component the Component to send
     * @since 3.2.0
     */
    public void sendMessage(@NonNull Component component) {
        if (sender != null) {
            sender.sendMessage(component);
        } else if (playerUUID != null) {
            Bukkit.getPluginManager().callEvent(
                    new OfflineMessageEvent(this.playerUUID, Util.componentToLegacy(component)));
        }
    }

    /**
     * Sends a translated message using MiniMessage TagResolvers for advanced formatting.
     * The translation is looked up by reference, then parsed with MiniMessage using the provided resolvers.
     *
     * @param reference - language file reference
     * @param resolvers - MiniMessage TagResolvers for placeholder substitution
     * @since 3.2.0
     */
    public void sendMiniMessage(@NonNull String reference, @NonNull TagResolver... resolvers) {
        Component component = getTranslationAsComponent(reference, resolvers);
        String plain = Util.componentToPlainText(component).trim();
        if (!plain.isEmpty()) {
            sendMessage(component);
        }
    }

    /**
     * Gets a translation as an Adventure Component, using MiniMessage TagResolvers.
     *
     * @param reference - language file reference
     * @param resolvers - MiniMessage TagResolvers
     * @return translated Component
     * @since 3.2.0
     */
    @NonNull
    public Component getTranslationAsComponent(@NonNull String reference, @NonNull TagResolver... resolvers) {
        String addonPrefix = addon == null ? ""
                : addon.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
        String raw = translate(addonPrefix, reference, new String[0]);
        // Convert legacy to MiniMessage if needed
        String mmText = Util.isLegacyFormat(raw) ? Util.legacyToMiniMessage(raw) : raw;
        return Util.parseMiniMessage(mmText, resolvers);
    }

    /**
     * Gets a translation as an Adventure Component, with variable substitution.
     *
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     * @return translated Component
     * @since 3.2.0
     */
    @NonNull
    public Component getTranslationAsComponent(@NonNull String reference, @NonNull String... variables) {
        String addonPrefix = addon == null ? ""
                : addon.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
        String raw = translate(addonPrefix, reference, variables);
        return Util.parseMiniMessageOrLegacy(raw);
    }

    /**
     * Sends a message to sender if message is not empty and if the same wasn't sent
     * within the previous Notifier.NOTIFICATION_DELAY seconds.
     * 
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     *
     * @see Notifier
     */
    public void notify(String reference, String... variables) {
        String message = getTranslation(reference, variables);
        if (!Util.stripColor(message).trim().isEmpty() && sender != null) {
            plugin.getNotifier().notify(this, message);
        }
    }

    /**
     * Sends a message to sender if message is not empty and if the same wasn't sent
     * within the previous Notifier.NOTIFICATION_DELAY seconds.
     * 
     * @param world     - the world the translation should come from
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     *
     * @see Notifier
     * @since 1.3.0
     */
    public void notify(World world, String reference, String... variables) {
        String message = getTranslation(world, reference, variables);
        if (!Util.stripColor(message).trim().isEmpty() && sender != null) {
            plugin.getNotifier().notify(this, message);
        }
    }

    /**
     * Sets the user's game mode
     * 
     * @param mode - GameMode
     */
    public void setGameMode(GameMode mode) {
        if (player != null) {
            player.setGameMode(mode);
        }
    }

    /**
     * Teleports user to this location. If the user is in a vehicle, they will exit
     * first.
     * 
     * @param location - the location
     */
    public void teleport(Location location) {
        if (player != null) {
            player.teleport(location);
        }
    }

    /**
     * Gets the current world this entity resides in
     * 
     * @return World - world
     */
    @NonNull
    public World getWorld() {
        Objects.requireNonNull(player, "Cannot be called on a non-player User!");
        return Objects.requireNonNull(player.getWorld(), "Player's world cannot be null!");
    }

    /**
     * Closes the user's inventory
     */
    public void closeInventory() {
        player.closeInventory();
    }

    /**
     * Get the user's locale
     * 
     * @return Locale
     */
    public Locale getLocale() {
        if (sender instanceof Player && !plugin.getPlayers().getLocale(playerUUID).isEmpty()) {
            return Locale.forLanguageTag(plugin.getPlayers().getLocale(playerUUID));
        }
        return Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage());
    }

    /**
     * Forces an update of the user's complete inventory. Deprecated, but there is
     * no current alternative.
     */
    public void updateInventory() {
        player.updateInventory();
    }

    /**
     * Performs a command as the player
     * 
     * @param command - command to execute
     * @return true if the command was successful, otherwise false
     */
    public boolean performCommand(String command) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(getPlayer(), command);
        Bukkit.getPluginManager().callEvent(event);

        // only perform the command, if the event wasn't cancelled by another plugin:
        if (!event.isCancelled()) {
            return getPlayer().performCommand(
                    event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage());
        }
        // Cancelled, but it was recognized, so return true
        return true;
    }

    /**
     * Checks if a user is in one of the game worlds
     * 
     * @return true if user is, false if not
     */
    public boolean inWorld() {
        return plugin.getIWM().inWorld(getLocation());
    }

    /**
     * Spawn particles to the player. They are only displayed if they are within the
     * server's view distance.
     * 
     * @param particle    Particle to display.
     * @param dustOptions Particle.DustOptions for the particle to display.
     * @param x           X coordinate of the particle to display.
     * @param y           Y coordinate of the particle to display.
     * @param z           Z coordinate of the particle to display.
     */
    public void spawnParticle(Particle particle, @Nullable Object dustOptions, double x, double y, double z) {
        Class<?> expectedClass = VALIDATION_CHECK.get(particle);
        if (expectedClass == null) {
            throw new IllegalArgumentException("Unexpected value: " + particle + "\nExpected one of:"
                    + VALIDATION_CHECK.keySet().stream().map(Particle::name).collect(Collectors.joining(", ")));
        }

        if (!(expectedClass.isInstance(dustOptions))) {
            throw new IllegalArgumentException("A non-null " + expectedClass.getSimpleName()
                    + " must be provided when using Particle." + particle + " as particle.");
        }

        // Check if this particle is beyond the viewing distance of the server
        if (this.player != null && this.player.getLocation().toVector().distanceSquared(new Vector(x, y,
                z)) < (Bukkit.getServer().getViewDistance() * 256 * Bukkit.getServer().getViewDistance())) {
            if (particle.equals(Enums.getIfPresent(Particle.class, "DUST")
                    .or(Enums.getIfPresent(Particle.class, "REDSTONE").or(Particle.FLAME)))) {
                player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 1, dustOptions);
            } else if (dustOptions != null) {
                player.spawnParticle(particle, x, y, z, 1, dustOptions);
            } else {
                // This will never be called unless the value in VALIDATION_CHECK is null in the
                // future
                player.spawnParticle(particle, x, y, z, 1);
            }
        }
    }

    /**
     * Spawn particles to the player. They are only displayed if they are within the
     * server's view distance. Compatibility method for older usages.
     * 
     * @param particle    Particle to display.
     * @param dustOptions Particle.DustOptions for the particle to display.
     * @param x           X coordinate of the particle to display.
     * @param y           Y coordinate of the particle to display.
     * @param z           Z coordinate of the particle to display.
     */
    public void spawnParticle(Particle particle, Particle.DustOptions dustOptions, double x, double y, double z) {
        this.spawnParticle(particle, (Object) dustOptions, x, y, z);
    }

    /**
     * Spawn particles to the player. They are only displayed if they are within the
     * server's view distance.
     * 
     * @param particle    Particle to display.
     * @param dustOptions Particle.DustOptions for the particle to display.
     * @param x           X coordinate of the particle to display.
     * @param y           Y coordinate of the particle to display.
     * @param z           Z coordinate of the particle to display.
     */
    public void spawnParticle(Particle particle, Particle.DustOptions dustOptions, int x, int y, int z) {
        this.spawnParticle(particle, dustOptions, (double) x, (double) y, (double) z);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playerUUID == null) ? 0 : playerUUID.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof User other)) {
            return false;
        }
        if (playerUUID == null) {
            return other.playerUUID == null;
        } else
            return playerUUID.equals(other.playerUUID);
    }

    /**
     * Set the addon context when a command is executed
     * 
     * @param addon - the addon executing the command
     */
    public void setAddon(Addon addon) {
        this.addon = addon;
    }

    /**
     * Get all the metadata for this user
     * 
     * @return the metaData
     * @since 1.15.4
     */
    @Override
    public Optional<Map<String, MetaDataValue>> getMetaData() {
        Players p = plugin.getPlayers().getPlayer(playerUUID);
        return Objects.requireNonNull(p, "Unknown player for " + playerUUID).getMetaData();
    }

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     */
    @Override
    public void setMetaData(Map<String, MetaDataValue> metaData) {
        Players p = plugin.getPlayers().getPlayer(playerUUID);

        Objects.requireNonNull(p, "Unknown player for " + playerUUID).setMetaData(metaData);
    }

    @Override
    public String toString() {
        return "User [" + (player != null ? "player=" + player + ", " : "")
                + (offlinePlayer != null ? "offlinePlayer=" + offlinePlayer + ", " : "")
                + (playerUUID != null ? "playerUUID=" + playerUUID + ", " : "")
                + (sender != null ? "sender=" + sender + ", " : "") + (addon != null ? "addon=" + addon + ", " : "")
                + "getLocation()=" + getLocation() + "isPlayer()=" + isPlayer()
                + "]";
    }

}
