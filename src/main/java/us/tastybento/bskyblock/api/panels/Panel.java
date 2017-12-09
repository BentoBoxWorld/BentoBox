package us.tastybento.bskyblock.api.panels;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import us.tastybento.bskyblock.listeners.PanelListener;

public class Panel {

    private Inventory inventory;
    private Map<Integer, PanelItem> items;

    public Panel(String name, Map<Integer, PanelItem> items) {
        this.items = items;

        // Create panel
        if (items.keySet().size() > 0) {
            // Make sure size is a multiple of 9
            int size = items.keySet().size() + 8;
            size -= (size % 9);
            inventory = Bukkit.createInventory(null, size, name);
            // Fill the inventory and return
            for (Map.Entry<Integer, PanelItem> en: items.entrySet()) {
                inventory.setItem(en.getKey(), en.getValue().getItem());
            }
        } else {
            inventory = Bukkit.createInventory(null, 9, name);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, PanelItem> getItems() {
        return items;
    }

    public void open(Player... players) {
        for (Player player : players) {
            player.openInventory(inventory);
            PanelListener.openPanels.put(player.getUniqueId(), this);
        }
    }
}
