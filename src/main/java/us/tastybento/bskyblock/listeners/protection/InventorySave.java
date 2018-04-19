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
    private static InventorySave instance = new InventorySave(BSkyBlock.getInstance());
    private HashMap<UUID, InventoryStore> inventories;

    /**
     * Saves the inventory of a player
     */
    public InventorySave(BSkyBlock plugin) {
        inventories = new HashMap<>();
    }

    /** Save player's inventory
     * @param player - the player
     */
    public void savePlayerInventory(Player player) {
        // Save the player's armor and things
        inventories.put(player.getUniqueId(),new InventoryStore(player.getInventory().getContents(), player.getInventory().getArmorContents()));
    }

    /**
     * Clears any saved inventory
     * @param player - the player
     */
    public void clearSavedInventory(Player player) {
        inventories.remove(player.getUniqueId());
    }
    /**
     * Load the player's inventory
     *
     * @param player - the player
     */
    public void loadPlayerInventory(Player player) {
        // Get the info for this player
        if (inventories.containsKey(player.getUniqueId())) {
            InventoryStore inv = inventories.get(player.getUniqueId());
            player.getInventory().setContents(inv.getInventory());
            player.getInventory().setArmorContents(inv.getArmor());
            inventories.remove(player.getUniqueId());
            return;
        }
    }

    public static InventorySave getInstance() {
        return instance;
    }

    /**
     * Returns whether the player's inventory has been stored to give him back.
     *
     * @param uuid - UUID UUID of the player
     * @return <code>true</code> if the inventory is stored, <code>false</code> otherwise
     */
    public static boolean isStored(UUID uuid) {
        return instance.inventories.containsKey(uuid);
    }
}
