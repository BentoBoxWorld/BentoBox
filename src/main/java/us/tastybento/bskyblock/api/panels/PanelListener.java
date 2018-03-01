package us.tastybento.bskyblock.api.panels;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import us.tastybento.bskyblock.api.user.User;

public interface PanelListener {

    /**
     * This is called when the panel is first setup
     */
    void setup();

    void onInventoryClose(InventoryCloseEvent event);

    void onInventoryClick(User user, InventoryClickEvent event);

}
