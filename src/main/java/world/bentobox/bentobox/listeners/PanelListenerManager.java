package world.bentobox.bentobox.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.InventoryView;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;

public class PanelListenerManager implements Listener {

    private static HashMap<UUID, Panel> openPanels = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        User user = User.getInstance(event.getWhoClicked()); // The player that clicked the item
        InventoryView view = event.getView();

        // Open the inventory panel that this player has open (they can only ever have one)
        if (openPanels.containsKey(user.getUniqueId()) &&
                openPanels.get(user.getUniqueId()).getInventory().equals(event.getInventory())) {
            // Cancel the event. If they don't want it to be cancelled then the click handler(s) should
            // uncancel it. If gui was from our environment, then cancel event anyway.
            event.setCancelled(true);

            // Check the name of the panel - strip colors. Note that black is removed from titles automatically by the server.
            if (ChatColor.stripColor(view.getTitle()).equals(ChatColor.stripColor(openPanels.get(user.getUniqueId()).getName()))) {
                // Close inventory if clicked outside and if setting is true
                if (BentoBox.getInstance().getSettings().isClosePanelOnClickOutside() && event.getSlotType().equals(SlotType.OUTSIDE)) {
                    event.getWhoClicked().closeInventory();
                    return;
                }

                // Get the panel itself
                Panel panel = openPanels.get(user.getUniqueId());
                // Check that they clicked on a specific item
                PanelItem pi = panel.getItems().get(event.getRawSlot());
                if (pi != null) {
                    pi.getClickHandler().ifPresent(handler ->
                    // Execute the handler's onClick method and optionally cancel the event if the handler returns true
                    event.setCancelled(handler.onClick(panel, user, event.getClick(), event.getSlot())));
                }
                // If there is a listener, then run it and refresh the panel
                panel.getListener().ifPresent(l -> {
                    l.onInventoryClick(user, event);
                    // Refresh
                    l.refreshPanel();
                });
            } else {
                // Wrong name - delete this panel
                openPanels.remove(user.getUniqueId());
                // This avoids GUIs being left open so that items can be taken.
                user.closeInventory();
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
        openPanels.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("BentoBox")) {
            closeAllPanels();
        }
    }

    /**
     * Closes all open BentoBox panels
     * @since 1.5.0
     */
    public static void closeAllPanels() {
        // Use stream clones to avoid concurrent modification exceptions
        openPanels.values().stream().collect(Collectors.toList()).forEach(p ->
        p.getInventory().getViewers().stream().collect(Collectors.toList()).forEach(HumanEntity::closeInventory));
    }

    /**
     * @return the openPanels
     */
    public static Map<UUID, Panel> getOpenPanels() {
        return openPanels;
    }

}
