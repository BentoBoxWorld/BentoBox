package us.tastybento.bskyblock.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.Panel;

public class PanelListenerManager implements Listener {

    //private static final boolean DEBUG = false;

    private static HashMap<UUID, Panel> openPanels = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        User user = User.getInstance(event.getWhoClicked()); // The player that
        // clicked the item
        //UUID playerUUID = player.getUniqueId();
        Inventory inventory = event.getInventory(); // The inventory that was
        // Open the inventory panel that this player has open (they can only ever have one)
        if (getOpenPanels().containsKey(user.getUniqueId())) {
            // Check the name of the panel
            if (inventory.getName().equals(getOpenPanels().get(user.getUniqueId()).getInventory().getName())) {
                // Get the panel itself
                Panel panel = getOpenPanels().get(user.getUniqueId());
                // Check that they clicked on a specific item
                for (int slot : panel.getItems().keySet()) {
                    if (slot == event.getRawSlot()) {
                        // Check that they left clicked on it
                        // TODO: in the future, we may want to support right clicking
                        panel.getItems().get(slot).getClickHandler().ifPresent(handler -> {
                            // Execute the handler's onClick method and optionally cancel the event if the handler returns true
                            event.setCancelled(handler.onClick(user, ClickType.LEFT));
                            // If there is a listener, then run it.
                            panel.getListener().ifPresent(l -> l.onInventoryClick(user, inventory, event.getCurrentItem()));
                        });
                    }
                }
            } else {
                // Wrong name - delete this panel
                getOpenPanels().remove(user.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (getOpenPanels().containsKey(event.getPlayer().getUniqueId())) getOpenPanels().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLogOut(PlayerQuitEvent event) {
        if (getOpenPanels().containsKey(event.getPlayer().getUniqueId())) getOpenPanels().remove(event.getPlayer().getUniqueId());
    }

    /**
     * @return the openPanels
     */
    public static HashMap<UUID, Panel> getOpenPanels() {
        return openPanels;
    }
    
}
