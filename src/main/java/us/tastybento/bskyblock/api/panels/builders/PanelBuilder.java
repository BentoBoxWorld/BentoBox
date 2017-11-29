package us.tastybento.bskyblock.api.panels.builders;

import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;

import java.util.HashMap;
import java.util.Map;

public class PanelBuilder {
    private String name;
    private Map<Integer, PanelItem> items = new HashMap<>();

    public PanelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PanelBuilder addItem(int slot, PanelItem item) {
        this.items.put(slot, item);
        return this;
    }

    public Panel build() {
        return new Panel(name, items);
    }
}
