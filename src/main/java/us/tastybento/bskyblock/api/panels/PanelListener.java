package us.tastybento.bskyblock.api.panels;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.commands.User;

public interface PanelListener {

    /**
     * This is called when the panel is first setup
     */
    void setup();

    /**
     * Called when the panel is clicked
     * @param user
     * @param inventory
     * @param clicked
     */
    void onInventoryClick(User user, Inventory inventory, ItemStack clicked);
}
