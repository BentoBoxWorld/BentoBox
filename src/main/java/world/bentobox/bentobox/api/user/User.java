package world.bentobox.bentobox.api.user;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.OfflineMessageEvent;
import world.bentobox.bentobox.util.Util;

/**
 * Combines {@link Player}, {@link OfflinePlayer} and {@link CommandSender} to provide convenience methods related to
 * localization and generic interactions.
 * <br/>
 * Therefore, a User could usually be a Player, an OfflinePlayer or the server's console.
 * Preliminary checks should be performed before trying to run methods that relies on a specific implementation.
 * <br/><br/>
 * It is good practice to use the User instance whenever possible instead of Player or CommandSender.
 *
 * @author tastybento
 */
public class User {

    private static Map<UUID, User> users = new HashMap<>();

    /**
     * Clears all users from the user list
     */
    public static void clearUsers() {
        users.clear();
    }

    /**
     * Gets an instance of User from a CommandSender
     * @param sender - command sender, e.g. console
     * @return user - user
     */
    @Nullable
    public static User getInstance(CommandSender sender) {
        if (sender instanceof Player) {
            return getInstance((Player)sender);
        }
        // Console
        return new User(sender);
    }

    /**
     * Gets an instance of User from a Player object
     * @param player - the player
     * @return user - user
     */
    @Nullable
    public static User getInstance(Player player) {
        if (player == null) {
            return null;
        }
        if (users.containsKey(player.getUniqueId())) {
            return users.get(player.getUniqueId());
        }
        return new User(player);
    }

    /**
     * Gets an instance of User from a UUID
     * @param uuid - UUID
     * @return user - user
     */
    @Nullable
    public static User getInstance(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (users.containsKey(uuid)) {
            return users.get(uuid);
        }
        // Return player, or null if they are not online
        return new User(uuid);
    }

    /**
     * Gets an instance of User from an OfflinePlayer
     * @param offlinePlayer offline Player
     * @return user
     * @since 1.3.0
     */
    @Nullable
    public static User getInstance(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return null;
        }
        if (users.containsKey(offlinePlayer.getUniqueId())) {
            return users.get(offlinePlayer.getUniqueId());
        }
        return new User(offlinePlayer);
    }

    /**
     * Removes this player from the User cache
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
    private Player player;
    private OfflinePlayer offlinePlayer;
    private final UUID playerUUID;
    @Nullable
    private final CommandSender sender;

    private Addon addon;

    private User(CommandSender sender) {
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
     * @param p - plugin
     */
    public static void setPlugin(BentoBox p) {
        plugin = p;
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return sender.getEffectivePermissions();
    }

    @Nullable
    public PlayerInventory getInventory() {
        return player != null ? player.getInventory() : null;
    }

    @Nullable
    public Location getLocation() {
        return player != null ? player.getLocation() : null;
    }

    public String getName() {
        return player != null ? player.getName() : plugin.getPlayers().getName(playerUUID);
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return true if this user is a player, false if not, e.g., console
     */
    public boolean isPlayer() {
        return player != null;
    }

    /**
     * @return the offline player
     * @since 1.3.0
     */
    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
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
     * @return true if permission is empty or null or if the player has that permission or if the player is op.
     */
    public boolean hasPermission(@Nullable String permission) {
        return permission == null || permission.isEmpty() || isOp() || sender.hasPermission(permission);
    }

    /**
     * Removes permission from user
     * @param name - Name of the permission to remove
     * @return true if successful
     * @since 1.5.0
     */
    public boolean removePerm(String name) {
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
     * @param name - Name of the permission to attach
     * @return The PermissionAttachment that was just created
     * @since 1.5.0
     */
    public PermissionAttachment addPerm(String name) {
        return player.addAttachment(plugin, name, true);
    }

    public boolean isOnline() {
        return player != null && player.isOnline();
    }

    /**
     * Checks if user is Op
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
     * Get the maximum value of a numerical permission setting.
     * If a player is given an explicit negative number then this is treated as "unlimited" and returned immediately.
     * @param permissionPrefix the start of the perm, e.g., {@code plugin.mypermission}
     * @param defaultValue the default value; the result may be higher or lower than this
     * @return max value
     */
    public int getPermissionValue(String permissionPrefix, int defaultValue) {
        // If requester is console, then return the default value
        if (!isPlayer()) return defaultValue;

        int value = 0;

        // If there is a dot at the end of the permissionPrefix, remove it
        if (permissionPrefix.endsWith(".")) {
            permissionPrefix = permissionPrefix.substring(0, permissionPrefix.length()-1);
        }

        final String permPrefix = permissionPrefix + ".";

        List<String> permissions = player.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue) // Must be a positive permission, not a negative one
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith(permPrefix))
                .collect(Collectors.toList());

        if (permissions.isEmpty()) return defaultValue;

        for (String permission : permissions) {
            if (permission.contains(permPrefix + "*")) {
                // 'Star' permission
                return defaultValue;
            } else {
                String[] spl = permission.split(permPrefix);
                if (spl.length > 1) {
                    if (!NumberUtils.isNumber(spl[1])) {
                        plugin.logError("Player " + player.getName() + " has permission: '" + permission + "' <-- the last part MUST be a number! Ignoring...");
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
     * @param world - world of translation
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go in pairs, for example
     *                  "[name]", "tastybento"
     * @return Translated string with colors converted, or the reference if nothing has been found
     * @since 1.3.0
     */
    public String getTranslation(World world, String reference, String... variables) {
        // Get translation.
        String addonPrefix = plugin.getIWM()
                .getAddon(world).map(a -> a.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".").orElse("");
        return translate(addonPrefix, reference, variables);
    }

    /**
     * Gets a translation of this reference for this user. Translations may be overridden by Addons
     * by using the same reference prefixed by the addon name (from the Addon Description) in lower case.
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go in pairs, for example
     *                  "[name]", "tastybento"
     * @return Translated string with colors converted, or the reference if nothing has been found
     */
    public String getTranslation(String reference, String... variables) {
        // Get addonPrefix
        String addonPrefix = addon == null ? "" : addon.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
        return translate(addonPrefix, reference, variables);
    }

    private String translate(String addonPrefix, String reference, String[] variables) {
        String translation = plugin.getLocalesManager().get(this, addonPrefix + reference);

        if (translation == null) {
            translation = plugin.getLocalesManager().get(this, reference);
            if (translation == null) {
                // If no translation has been found, return the reference for debug purposes.
                return reference;
            }
        }

        // If this is a prefix, just gather and return the translation
        if (reference.startsWith("prefixes.")) {
            return translation;
        } else {
            // Replace the prefixes
            for (String prefix : plugin.getLocalesManager().getAvailablePrefixes(this)) {
                String prefixTranslation = getTranslation("prefixes." + prefix);
                // Replace the [gamemode] text variable
                prefixTranslation = prefixTranslation.replace("[gamemode]", addon != null ? addon.getDescription().getName() : "[gamemode]");
                // Replace the [friendly_name] text variable
                prefixTranslation = prefixTranslation.replace("[friendly_name]", getWorld() != null ? plugin.getIWM().getFriendlyName(getWorld()) : "[friendly_name]");

                // Replace the prefix in the actual message
                translation = translation.replace("[prefix_" + prefix + "]", prefixTranslation);
            }

            // Then replace variables
            if (variables.length > 1) {
                for (int i = 0; i < variables.length; i += 2) {
                    translation = translation.replace(variables[i], variables[i + 1]);
                }
            }

            // Then replace Placeholders, this will only work if this is a player
            if (player != null) {
                translation = plugin.getPlaceholdersManager().replacePlaceholders(player, translation);
            }

            return Util.stripSpaceAfterColorCodes(ChatColor.translateAlternateColorCodes('&', translation));
        }
    }

    /**
     * Gets a translation of this reference for this user.
     * @param reference - reference found in a locale file
     * @param variables - variables to insert into translated string. Variables go in pairs, for example
     *                  "[name]", "tastybento"
     * @return Translated string with colors converted, or a blank String if nothing has been found
     */
    public String getTranslationOrNothing(String reference, String... variables) {
        String translation = getTranslation(reference, variables);
        return translation.equals(reference) ? "" : translation;
    }

    /**
     * Send a message to sender if message is not empty.
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     */
    public void sendMessage(String reference, String... variables) {
        String message = getTranslation(reference, variables);
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            sendRawMessage(message);
        }
    }

    /**
     * Sends a message to sender without any modification (colors, multi-lines, placeholders).
     * @param message - the message to send
     */
    public void sendRawMessage(String message) {
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            // Offline player fire event
            Bukkit.getPluginManager().callEvent(new OfflineMessageEvent(this.playerUUID, message));
        }
    }

    /**
     * Sends a message to sender if message is not empty and if the same wasn't sent within the previous Notifier.NOTIFICATION_DELAY seconds.
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     *
     * @see Notifier
     */
    public void notify(String reference, String... variables) {
        String message = getTranslation(reference, variables);
        if (!ChatColor.stripColor(message).trim().isEmpty() && sender != null) {
            plugin.getNotifier().notify(this, message);
        }
    }

    /**
     * Sends a message to sender if message is not empty and if the same wasn't sent within the previous Notifier.NOTIFICATION_DELAY seconds.
     * @param world - the world the translation should come from
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     *
     * @see Notifier
     * @since 1.3.0
     */
    public void notify(World world, String reference, String... variables) {
        String message = getTranslation(world, reference, variables);
        if (!ChatColor.stripColor(message).trim().isEmpty() && sender != null) {
            plugin.getNotifier().notify(this, message);
        }
    }

    /**
     * Sets the user's game mode
     * @param mode - GameMode
     */
    public void setGameMode(GameMode mode) {
        player.setGameMode(mode);
    }

    /**
     * Teleports user to this location. If the user is in a vehicle, they will exit first.
     * @param location - the location
     */
    public void teleport(Location location) {
        player.teleport(location);
    }

    /**
     * Gets the current world this entity resides in
     * @return World - world or null
     */
    public World getWorld() {
        return player == null ? null : player.getWorld();
    }

    /**
     * Closes the user's inventory
     */
    public void closeInventory() {
        player.closeInventory();
    }

    /**
     * Get the user's locale
     * @return Locale
     */
    public Locale getLocale() {
        if (sender instanceof Player && !plugin.getPlayers().getLocale(playerUUID).isEmpty()) {
            return Locale.forLanguageTag(plugin.getPlayers().getLocale(playerUUID));
        }
        return Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage());
    }

    /**
     * Forces an update of the user's complete inventory.
     * Deprecated, but there is no current alternative.
     */
    public void updateInventory() {
        player.updateInventory();
    }

    /**
     * Performs a command as the player
     * @param command - command to execute
     * @return true if the command was successful, otherwise false
     */
    public boolean performCommand(String command) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(getPlayer(), command);
        Bukkit.getPluginManager().callEvent(event);

        // only perform the command, if the event wasn't cancelled by an other plugin:
        if (!event.isCancelled()) {
            return getPlayer().performCommand(event.getMessage());
        }
        // Cancelled, but it was recognized, so return true
        return true;
    }

    /**
     * Checks if a user is in one of the game worlds
     * @return true if user is, false if not
     */
    public boolean inWorld() {
        return plugin.getIWM().inWorld(getLocation());
    }

    /**
     * Spawn particles to the player.
     * They are only displayed if they are within the server's view distance.
     * @param particle Particle to display.
     * @param dustOptions Particle.DustOptions for the particle to display.
     *                    Cannot be null when particle is {@link Particle#REDSTONE}.
     * @param x X coordinate of the particle to display.
     * @param y Y coordinate of the particle to display.
     * @param z Z coordinate of the particle to display.
     */
    public void spawnParticle(Particle particle, Particle.DustOptions dustOptions, double x, double y, double z) {
        if (particle.equals(Particle.REDSTONE) && dustOptions == null) {
            // Security check that will avoid later unexpected exceptions.
            throw new IllegalArgumentException("A non-null Particle.DustOptions must be provided when using Particle.REDSTONE as particle.");
        }

        // Check if this particle is beyond the viewing distance of the server
        if (player.getLocation().toVector().distanceSquared(new Vector(x,y,z)) < (Bukkit.getServer().getViewDistance()*256*Bukkit.getServer().getViewDistance())) {
            if (particle.equals(Particle.REDSTONE)) {
                player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 1, dustOptions);
            } else {
                player.spawnParticle(particle, x, y, z, 1);
            }
        }
    }

    /**
     * Spawn particles to the player.
     * They are only displayed if they are within the server's view distance.
     * @param particle Particle to display.
     * @param dustOptions Particle.DustOptions for the particle to display.
     *                    Cannot be null when particle is {@link Particle#REDSTONE}.
     * @param x X coordinate of the particle to display.
     * @param y Y coordinate of the particle to display.
     * @param z Z coordinate of the particle to display.
     */
    public void spawnParticle(Particle particle, Particle.DustOptions dustOptions, int x, int y, int z) {
        spawnParticle(particle, dustOptions, (double) x, (double) y, (double) z);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playerUUID == null) ? 0 : playerUUID.hashCode());
        return result;
    }

    /* (non-Javadoc)
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
        if (!(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        if (playerUUID == null) {
            return other.playerUUID == null;
        } else return playerUUID.equals(other.playerUUID);
    }

    /**
     * Set the addon context when a command is executed
     * @param addon - the addon executing the command
     */
    public void setAddon(Addon addon) {
        this.addon = addon;
    }
}
