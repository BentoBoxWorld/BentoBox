package us.tastybento.bskyblock.listeners.protection;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Stashes inventories when required for a player
 * 
 * @author tastybento
 * 
 */
public class InventorySave {
    private static InventorySave instance = new InventorySave(BSkyBlock.getPlugin());
    private HashMap<UUID, InventoryStore> inventories;

    /**
     * Saves the inventory of a player
     */
    public InventorySave(BSkyBlock plugin) {
        inventories = new HashMap<UUID, InventoryStore>();
    }

    /** Save player's inventory
     * @param player
     */
    public void savePlayerInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Saving inventory");
        // Save the player's armor and things
        inventories.put(player.getUniqueId(),new InventoryStore(player.getInventory().getContents(), player.getInventory().getArmorContents()));
    }

    /**
     * Clears any saved inventory
     * @param player
     */
    public void clearSavedInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Clearing inventory");
        inventories.remove(player.getUniqueId());
    }
    /**
     * Load the player's inventory
     * 
     * @param player
     */
    public void loadPlayerInventory(Player player) {
        //plugin.getLogger().info("DEBUG: Loading inventory");
        // Get the info for this player
        if (inventories.containsKey(player.getUniqueId())) {
            InventoryStore inv = inventories.get(player.getUniqueId());
            //plugin.getLogger().info("DEBUG: player is known");
            player.getInventory().setContents(inv.getInventory());
            player.getInventory().setArmorContents(inv.getArmor());
            inventories.remove(player.getUniqueId());
            return;
        }
    }

    public static InventorySave getInstance() {
        return instance;
    }

}
