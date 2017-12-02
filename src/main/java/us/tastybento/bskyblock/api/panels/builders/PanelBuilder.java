package us.tastybento.bskyblock.api.panels.builders;

import java.util.TreeMap;

import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;

public class PanelBuilder {
    private String name;
    private TreeMap<Integer, PanelItem> items = new TreeMap<>();

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
    public Panel build() {
        return new Panel(name, items);
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
}
