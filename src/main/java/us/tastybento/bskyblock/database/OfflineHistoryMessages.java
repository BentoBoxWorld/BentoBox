package us.tastybento.bskyblock.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;

/**
 * Handles offline messaging to players and teams
 * 
 * @author Tastybento
 */
public class OfflineHistoryMessages {
    private BSkyBlock plugin;
    private BSBDatabase database;

    // Offline Messages
    private HashMap<UUID, List<String>> messages;

    public OfflineHistoryMessages(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        messages = new HashMap<UUID, List<String>>();
    }

    public void load(){
        messages = database.loadOfflineHistoryMessages();
    }

    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    database.saveOfflineHistoryMessages(messages);
                }
            });
        } else {
            database.saveOfflineHistoryMessages(messages);
        }
    }

    public void shutdown(){
        save(false);
        messages.clear();
    }

    /**
     * History messages types that allow filtering them.
     * - TEAM : invited coop, removed coop, challenge completed, player left team, player (un)banned
     * - ISLAND : level, island range increased, (admin) locked/unlocked, biome set by admin, warp removed
     * - DEATH : player died (only if death penalty enabled)
     * - PERSONAL : coop removed, (un)banned, kicked from an island, now a leader
     */
    public enum HistoryMessageType{
        TEAM,
        ISLAND,
        DEATH,
        PERSONAL;
    }

    /**
     * Returns what messages are waiting for the player or null if none
     * 
     * @param playerUUID
     * @return List of messages
     */
    public List<String> getMessages(UUID playerUUID){
        return messages.get(playerUUID);
    }

    /**
     * Clears any messages for player
     * 
     * @param playerUUID
     */
    public void clearMessages(UUID playerUUID) {
        messages.remove(playerUUID);
    }

    /**
     * Sets a message for the player to receive next time they login
     * 
     * @param playerUUID
     * @param type
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, HistoryMessageType type, String message) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        // Check if player is online. If so, return false
        if (player != null && player.isOnline()) {
            return false;
        }
        storeMessage(playerUUID, type, message);
        return true;
    }

    /**
     * Stores a message without any online check
     * @param playerUUID
     * @param type
     * @param message
     */
    public void storeMessage(UUID playerUUID, HistoryMessageType type, String message) {
        if(Settings.historyMessagesTypes.contains(type)){
            List<String> playerMessages = getMessages(playerUUID);
            if (playerMessages != null) {
                playerMessages.add(message);
            } else {
                playerMessages = new ArrayList<String>(Arrays.asList(message));
            }
            messages.put(playerUUID, playerMessages);
        }
    }
}
