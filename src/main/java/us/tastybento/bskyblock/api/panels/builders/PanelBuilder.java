package us.tastybento.bskyblock.api.panels.builders;

import java.util.Optional;
import java.util.TreeMap;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelListener;

public class PanelBuilder {
    private String name;
    private TreeMap<Integer, PanelItem> items = new TreeMap<>();
    private int size;
    private Optional<User> user;
    private Optional<PanelListener> listener;

    public PanelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add item into a specific slot. If it is already occupied, it will be replaced.
     * @param slot - slot
     * @param item - Panel item
     * @return PanelBuilder
     */
    public PanelBuilder addItem(int slot, PanelItem item) {
        this.items.put(slot, item);
        return this;
    }

    public int nextSlot() {
        if (this.items.isEmpty()) {
            return 0;
        } else {
            return items.lastEntry().getKey() + 1;
        }  
    }
    
    /**
     * Checks if a slot is occupied in the panel or not
     * @param slot to check
     * @return true or false
     */
    public boolean slotOccupied(int slot) {
        return this.items.containsKey(slot);
    }
    
    /**
     * Build the panel
     * @return Panel
     */
    public Panel build() {
        return new Panel(name, items, size, user, listener);
    }

    /**
     * Add item to the panel in the last slot.
     * @param item - Panel item
     * @return PanelBuilder
     */
    public PanelBuilder addItem(PanelItem item) {
        if (items.isEmpty()) {
            this.items.put(0, item);
        } else {
            this.items.put(items.lastEntry().getKey() + 1, item);
        }
        return this;
    }

    /**
     * Forces panel to be a specific number of slots.
     * @param size
     * @return PanelBuilder
     */
    public PanelBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * Sets the user who will get this panel. This will open it immediately when it is built
     * @param user
     * @return PanelBuilder
     */
    public PanelBuilder setUser(User user) {
        this.user = Optional.of(user);
        return this;
    }

    /**
     * Sets which PanelListener will listen for clicks
     * @param listener
     * @return PanelBuilder
     */
    public PanelBuilder setListener(PanelListener listener) {
        this.listener = Optional.of(listener);
        return this;
    }
}
