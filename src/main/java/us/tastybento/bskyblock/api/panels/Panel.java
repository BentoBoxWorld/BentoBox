package us.tastybento.bskyblock.api.panels;

import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.listeners.PanelListenerManager;

public class Panel {

    private Inventory inventory;
    private Map<Integer, PanelItem> items;
    private Optional<PanelListener> listener;
    private Optional<User> user;

    public Panel(String name, Map<Integer, PanelItem> items, int size, Optional<User> user, Optional<PanelListener> listener) {
        this.items = items;
        // If size is undefined (0) then use the number of items
        if (size == 0) {
            size = items.keySet().size();
        } 
        // Create panel
        if (size > 0) {
            // Make sure size is a multiple of 9
            size = size + 8;
            size -= (size % 9);
            inventory = Bukkit.createInventory(null, size, name);
            // Fill the inventory and return
            for (Map.Entry<Integer, PanelItem> en: items.entrySet()) {
                inventory.setItem(en.getKey(), en.getValue().getItem());
            }
        } else {
            inventory = Bukkit.createInventory(null, 9, name);
        }
        this.listener = listener;
        // If the listener is defined, then run setup
        listener.ifPresent(l -> l.setup());
        /*
        if (listener.isPresent()) {
            listener.get().setup();
        }*/
        // If the user is defined, then open panel immediately
        this.user = user;
        user.ifPresent(this::open);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, PanelItem> getItems() {
        return items;
    }

    /**
     * @return the listener
     */
    public Optional<PanelListener> getListener() {
        return listener;
    }

    public Optional<User> getUser() {
        return user;
    }

    public void open(Player... players) {
        for (Player player : players) {
            player.openInventory(inventory);
            PanelListenerManager.openPanels.put(player.getUniqueId(), this);
        }
    }

    /**
     * Open the inventory panel
     * @param users
     */
    public void open(User... users) {
        for (User user : users) {
            user.getPlayer().openInventory(inventory);
            PanelListenerManager.openPanels.put(user.getUniqueId(), this);
        }
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * @param items the items to set
     */
    public void setItems(Map<Integer, PanelItem> items) {
        this.items = items;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(Optional<PanelListener> listener) {
        this.listener = listener;
    }

    /**
     * @param user the user to set
     */
    public void setUser(Optional<User> user) {
        this.user = user;
    }
}
