package us.tastybento.bskyblock.api.commands;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;

/**
 * BSB's user object. Wraps Player.
 * @author tastybento
 *
 */
public class User {

    private static Map<UUID, User> users = new HashMap<>();
    /**
     * Get an instance of User from a CommandSender
     * @param sender
     * @return user
     */
    public static User getInstance(CommandSender sender) {
        if (sender instanceof Player) {
            return getInstance((Player)sender);
        }
        // Console
        return new User(sender);
    }
    /**
     * Get an instance of User from a Player object
     * @param player
     * @return user
     */
    public static User getInstance(Player player) {
        if (player == null)
            return null;
        if (users.containsKey(player.getUniqueId())) {
            return users.get(player.getUniqueId());
        }
        return new User(player);
    }
    /**
     * Get an instance of User from a UUID
     * @param uuid
     * @return user
     */
    public static User getInstance(UUID uuid) {
        if (users.containsKey(uuid)) {
            return users.get(uuid);
        }
        // Return player, or null if they are not online
        return new User(uuid);
    }
    /**
     * Removes this player from the User cache
     * @param player
     */
    public static void removePlayer(Player player) {
        users.remove(player.getUniqueId());
    }

    // ----------------------------------------------------

    private final BSkyBlock plugin = BSkyBlock.getInstance();

    private final Player player;
    private final UUID playerUUID;
    private final CommandSender sender;

    private User(CommandSender sender) {
        this.player = null;
        this.playerUUID = null;
        this.sender = sender;
    }

    private User(Player player) {
        this.player = player;
        this.sender = player;
        this.playerUUID = player.getUniqueId();
        users.put(player.getUniqueId(), this);
    }

    private User(UUID playerUUID) {
        this.player = Bukkit.getPlayer(playerUUID);
        this.playerUUID = playerUUID;
        this.sender = null;
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return sender.getEffectivePermissions();
    }

    public PlayerInventory getInventory() {
        return player.getInventory();
    }

    public Location getLocation() {
        return player.getLocation();
    }

    public String getName() {
        return player.getName();
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    public boolean isPlayer() {
        return player != null;
    }

    public CommandSender getSender() {
        return sender;
    }

    public UUID getUniqueId() {
        return playerUUID;
    }

    /**
     * @param permission
     * @return true if permission is empty or if the player has that permission
     */
    public boolean hasPermission(String permission) {
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    public boolean isOnline() {
        return player != null && player.isOnline();
    }

    public boolean isOp() {
        return sender.isOp();
    }

    /**
     * Gets a translation for this user
     * @param reference
     * @param variables
     * @return Translated string with colors converted
     */
    public String getTranslation(String reference, String... variables) {
        String translation = plugin.getLocalesManager().get(this, reference);
        if (variables.length > 1) {
            for (int i = 0; i < variables.length; i += 2) {
                translation = translation.replace(variables[i], variables[i+1]);
            }
        }
        
        return translation == null ? reference : ChatColor.translateAlternateColorCodes('&', translation);
    }

    /**
     * Returns blank string if reference is not available
     * @param reference
     * @param variables
     * @return 
     */
    public String getTranslationOrNothing(String reference, String... variables) {
        String translation = getTranslation(reference, variables);
        return translation == null ? "" : translation;
    }
    
    /**
     * Send a message to sender if message is not empty. Does not include color codes or spaces.
     * @param reference - language file reference
     * @param variables - CharSequence target, replacement pairs
     */
    public void sendMessage(String reference, String... variables) {
        String message = getTranslation(reference, variables);
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            if (sender != null) {
                sender.sendMessage(message);
            } else {
                // TODO: Offline message
                // Save this message so the player can see it later
            }
        }
    }
    
    /**
     * Sends a message to sender without any modification (colors, multi-lines, placeholders).
     * Should only be used for debug purposes.
     * @param message - the message to send
     */
    public void sendRawMessage(String message) {
        if (sender != null) {
            sender.sendMessage(message);
        } else {
            // TODO: Offline message
            // Save this message so the player can see it later
        }
    }

    /**
     * Sets the user's game mode
     * @param mode
     */
    public void setGameMode(GameMode mode) {
        player.setGameMode(mode);
    }

    /**
     * Teleports user to this location. If the user is in a vehicle, they will exit first.
     * @param location
     */
    public void teleport(Location location) {
        player.teleport(location);
    }

    /**
     * Gets the current world this entity resides in
     * @return World
     */
    public World getWorld() {
        return player.getWorld();
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
        if (sender instanceof Player) {
            if (!plugin.getPlayers().getLocale(this.playerUUID).isEmpty())
                return Locale.forLanguageTag(plugin.getPlayers().getLocale(this.playerUUID));
        } 
        return Locale.forLanguageTag(Settings.defaultLanguage);

    }
}
