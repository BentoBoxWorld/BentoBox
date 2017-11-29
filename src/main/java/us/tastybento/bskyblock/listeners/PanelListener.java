package us.tastybento.bskyblock.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;

import java.util.HashMap;
import java.util.UUID;

public class PanelListener implements Listener {

    private static final boolean DEBUG = false;
    private BSkyBlock plugin;

    public static HashMap<UUID, Panel> openPanels = new HashMap<>();

    public PanelListener(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked(); // The player that
        // clicked the item
        //UUID playerUUID = player.getUniqueId();
        Inventory inventory = event.getInventory(); // The inventory that was

        if (openPanels.containsKey(player.getUniqueId())) {
            if (inventory.getName().equals(openPanels.get(player.getUniqueId()).getInventory().getName())) {
                Panel panel = openPanels.get(player.getUniqueId());

                for (int slot : panel.getItems().keySet()) {
                    if (slot == event.getRawSlot()) {
                        if(!panel.getItems().get(slot).getClickHandler().onClick(player, ClickType.LEFT)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (openPanels.containsKey(event.getPlayer().getUniqueId())) openPanels.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLogOut(PlayerQuitEvent event) {
        if (openPanels.containsKey(event.getPlayer().getUniqueId())) openPanels.remove(event.getPlayer().getUniqueId());
    }
}
