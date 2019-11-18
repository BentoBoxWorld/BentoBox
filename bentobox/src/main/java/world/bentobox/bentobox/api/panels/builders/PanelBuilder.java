package world.bentobox.bentobox.api.panels.builders;

import java.util.SortedMap;
import java.util.TreeMap;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;

/**
 * Builds panels
 * @author tastybento
 *
 */
public class PanelBuilder {
    private String name;
    private final SortedMap<Integer, PanelItem> items = new TreeMap<>();
    private int size;
    private User user;
    private PanelListener listener;
    private Panel.Type type = Panel.Type.INVENTORY;

    public PanelBuilder name(String name) {
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        return this;
    }

    /**
     * Add item to the panel in the last slot.
     * @param item - Panel item
     * @return PanelBuilder
     */
    public PanelBuilder item(PanelItem item) {
        // Do not add null items
        if (item == null) {
            return this;
        }
        return item(nextSlot(), item);
    }

    /**
     * Add item into a specific slot. If it is already occupied, it will be replaced.
     * @param slot - slot
     * @param item - Panel item
     * @return PanelBuilder
     */
    public PanelBuilder item(int slot, PanelItem item) {
        // Do not add null items
        if (item == null) {
            return this;
        }
        items.put(slot, item);
        return this;
    }

    /**
     * Forces panel to be a specific number of slots.
     * @param size - size to be
     * @return PanelBuilder - PanelBuilder
     */
    public PanelBuilder size(int size) {
        this.size = size;
        return this;
    }

    /**
     * Sets the user who will get this panel. This will open it immediately when it is built
     * @param user - the User
     * @return PanelBuilder
     */
    public PanelBuilder user(User user) {
        this.user = user;
        return this;
    }

    /**
     * Sets which PanelListener will listen for clicks
     * @param listener - listener for this panel
     * @return PanelBuilder
     */
    public PanelBuilder listener(PanelListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Sets which Panel.Type will be used.
     * Defaults to {@link Panel.Type#INVENTORY}.
     * @param type - Panel.Type for this panel.
     * @return PanelBuilder
     * @since 1.7.0
     */
    public PanelBuilder type(Panel.Type type) {
        this.type = type;
        return this;
    }

    /**
     * Get the next free slot number after the largest slot.
     * @return next slot number, or -1 in case none has been found.
     */
    public int nextSlot() {
        return items.isEmpty() ? 0 : items.lastKey() + 1;
    }

    /**
     * Checks if a slot is occupied in the panel or not
     * @param slot to check
     * @return true or false
     */
    public boolean slotOccupied(int slot) {
        return items.containsKey(slot);
    }

    /**
     * Build the panel
     * @return Panel
     */
    public Panel build() {
        // items.lastKey() is a slot position, so the panel size is this value + 1
        return new Panel(name, items, Math.max(size, items.isEmpty() ? size : items.lastKey() + 1), user, listener, type);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the items
     */
    public SortedMap<Integer, PanelItem> getItems() {
        return items;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the listener
     */
    public PanelListener getListener() {
        return listener;
    }


    /**
     * @return the panelType
     * @since 1.7.0
     */
    public Panel.Type getPanelType() {
        return type;
    }
}
