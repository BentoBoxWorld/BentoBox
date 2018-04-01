package us.tastybento.bskyblock.api.panels;

import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.listeners.PanelListenerManager;
import us.tastybento.bskyblock.util.HeadGetter;
import us.tastybento.bskyblock.util.HeadRequester;

public class Panel implements HeadRequester {

    private Inventory inventory;
    private Map<Integer, PanelItem> items;
    private PanelListener listener;
    private User user;

    public Panel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener) {
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
                //TODO allow multi-paging
                if (en.getKey() < 54) inventory.setItem(en.getKey(), en.getValue().getItem());
                // Get player head async
                if (en.getValue().isPlayerHead()) {
                    HeadGetter.getHead(en.getValue(), this);
                }
            }
        } else {
            inventory = Bukkit.createInventory(null, 9, name);
        }
        this.listener = listener;
        // If the listener is defined, then run setup
        if (listener != null) listener.setup();

        // If the user is defined, then open panel immediately
        this.user = user;
        if (user != null) this.open(user);
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
        return Optional.ofNullable(listener);
    }

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    public void open(Player... players) {
        for (Player player : players) {
            player.openInventory(inventory);
            PanelListenerManager.getOpenPanels().put(player.getUniqueId(), this);
        }
    }

    /**
     * Open the inventory panel
     * @param users
     */
    public void open(User... users) {
        for (User u : users) {
            u.getPlayer().openInventory(inventory);
            PanelListenerManager.getOpenPanels().put(u.getUniqueId(), this);
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
    public void setListener(PanelListener listener) {
        this.listener = listener;
    }

    /**
     * @param user - the User the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public void setHead(PanelItem item) {
        // Update the panel item
        items.values().stream().filter(i -> i.getName().equals(item.getName())).forEach(i -> i = item);
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack it = inventory.getItem(i);
            if (it != null && it.getType().equals(Material.SKULL_ITEM)) {
                ItemMeta meta = it.getItemMeta();
                if (item.getName().equals(meta.getLocalizedName())) {
                    inventory.setItem(i, item.getItem());
                    return;
                }
            }
        }
    }
}
