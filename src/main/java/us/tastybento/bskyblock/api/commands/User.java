package us.tastybento.bskyblock.api.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.BSkyBlock;

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

    private final Player player;

    private final UUID playerUUID;
    
    private final BSkyBlock plugin = BSkyBlock.getPlugin();

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
        return player.getEffectivePermissions();
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

    public CommandSender getSender() {
        return sender;
    }

    public UUID getUniqueId() {
        return playerUUID;
    }

    public boolean hasPermission(String string) {
        return player.hasPermission(string);
    }

    public boolean isOnline() {
        return player == null ? false : player.isOnline();
    }

    public boolean isOp() {
        return player.isOp();
    }

    /**
     * Send a message to sender if message is not empty. Does not include color codes or spaces
     * @param reference - language file reference. May be preceded with color codes.
     * @param variables - CharSequence target, replacement pairs
     */
    public void sendMessage(String reference, String... variables ) {
        String message = ChatColor.getLastColors(reference) + plugin.getLocale(sender).get(ChatColor.stripColor(reference));
        if (variables.length > 1) {
            for (int i = 0; i < variables.length; i+=2) {
                message.replace(variables[i], variables[i+1]);
            }
        }
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            if (sender != null) {
                sender.sendMessage(message);
            } else {
                // TODO: Offline message
                // Save this message so the player can see it later
            }
        }
    }

    public void setGameMode(GameMode mode) {
        player.setGameMode(mode);
        
    }

    public void teleport(Location location) {
        player.teleport(location);
        
    }
}
