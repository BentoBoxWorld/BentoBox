package world.bentobox.bentobox.api.panels;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.util.heads.HeadGetter;
import world.bentobox.bentobox.util.heads.HeadRequester;

/**
 * A GUI panel that uses the Bukkit inventory API
 * @author tastybento
 *
 */
public class Panel implements HeadRequester, InventoryHolder {

    private Inventory inventory;
    private Map<Integer, PanelItem> items;
    private PanelListener listener;
    private User user;
    private String name;

    /**
     * Various types of Panel that can be created.
     * @since 1.7.0
     */
    public enum Type {
        INVENTORY,
        HOPPER,
        DROPPER
    }

    public Panel() {}

    public Panel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener) {
        this(name, items, size, user, listener, Type.INVENTORY);
    }

    /**
     * @since 1.7.0
     */
    public Panel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener, Type type) {
        makePanel(name, items, size, user, listener, type);
    }

    protected void makePanel(String name, Map<Integer, PanelItem> items, int size, User user,
            PanelListener listener) {
        this.makePanel(name, items, size, user, listener, Type.INVENTORY);
    }

    /**
     * @since 1.7.0
     */
    protected void makePanel(String name, Map<Integer, PanelItem> items, int size, User user,
            PanelListener listener, Type type) {
        this.name = name;
        this.items = items;

        // Create panel
        switch (type) {
        case INVENTORY:
            inventory = Bukkit.createInventory(null, fixSize(size), name);
            break;
        case HOPPER:
            inventory = Bukkit.createInventory(null, InventoryType.HOPPER, name);
            break;
        case DROPPER:
            inventory = Bukkit.createInventory(null, InventoryType.DROPPER, name);
            break;
        }

        // Fill the inventory and return
        for (Map.Entry<Integer, PanelItem> en: items.entrySet()) {
            if (en.getKey() < 54) {
                inventory.setItem(en.getKey(), en.getValue().getItem());
                // Get player head async
                if (en.getValue().isPlayerHead()) {
                    HeadGetter.getHead(en.getValue(), this);
                }
            }
        }
        this.listener = listener;
        // If the listener is defined, then run setup
        if (listener != null) listener.setup();

        // If the user is defined, then open panel immediately
        this.user = user;
        if (user != null) this.open(user);
    }

    private int fixSize(int size) {
        // If size is undefined (0) then use the number of items
        if (size == 0) {
            size = items.keySet().size();
        }
        if (size > 0) {
            // Make sure size is a multiple of 9 and is 54 max.
            size = size + 8;
            size -= (size % 9);
            if (size > 54) size = 54;
        } else {
            return 9;
        }
        return size;
    }

    @NonNull
    @Override
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
     * @param users - users that should see the panel
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

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.util.heads.HeadRequester#setHead(world.bentobox.bentobox.api.panels.PanelItem)
     */
    @Override
    public void setHead(PanelItem item) {
        // Update the panel item
        // Replace the item in the item list if the name is the same
        items = items.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (item.getName().equals(e.getValue().getName()) ? item : e.getValue())));
        // Replace the inventory slot item
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack it = inventory.getItem(i);
            if (it != null && it.getType().equals(Material.PLAYER_HEAD)) {
                ItemMeta meta = it.getItemMeta();
                if (meta != null && ChatColor.stripColor(item.getName()).equals(ChatColor.stripColor(meta.getLocalizedName()))) {
                    inventory.setItem(i, item.getItem());
                }
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
