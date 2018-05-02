package us.tastybento.bskyblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.user.User;

public class PanelListenerManager implements Listener {

    private static HashMap<UUID, Panel> openPanels = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        User user = User.getInstance(event.getWhoClicked()); // The player that
        // clicked the item
        Inventory inventory = event.getInventory(); // The inventory that was
        // Open the inventory panel that this player has open (they can only ever have one)
        if (openPanels.containsKey(user.getUniqueId())) {
            // Check the name of the panel
            if (inventory.getName().equals(openPanels.get(user.getUniqueId()).getInventory().getName())) {
                // Cancel the event. If they don't want it to be canceled then the click handler(s) should uncancel it
                event.setCancelled(true);
                // Get the panel itself
                Panel panel = openPanels.get(user.getUniqueId());
                // Check that they clicked on a specific item
                for (int slot : panel.getItems().keySet()) {
                    if (slot == event.getRawSlot()) {
                        panel.getItems().get(slot).getClickHandler().ifPresent(handler ->
                            // Execute the handler's onClick method and optionally cancel the event if the handler returns true
                            event.setCancelled(handler.onClick(panel, user, event.getClick(), event.getSlot()))
                        );
                    }
                }
                // If there is a listener, then run it.
                panel.getListener().ifPresent(l -> l.onInventoryClick(user, event));

            } else {
                // Wrong name - delete this panel
                openPanels.remove(user.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (openPanels.containsKey(event.getPlayer().getUniqueId())) {
            // Run any close inventory methods
            openPanels.get(event.getPlayer().getUniqueId()).getListener().ifPresent(l -> l.onInventoryClose(event));
            openPanels.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLogOut(PlayerQuitEvent event) {
        if (openPanels.containsKey(event.getPlayer().getUniqueId())) {
            openPanels.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * @return the openPanels
     */
    public static Map<UUID, Panel> getOpenPanels() {
        return openPanels;
    }

}
