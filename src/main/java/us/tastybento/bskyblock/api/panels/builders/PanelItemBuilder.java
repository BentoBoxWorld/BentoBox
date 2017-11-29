package us.tastybento.bskyblock.api.panels.builders;

import org.bukkit.inventory.ItemStack;
import us.tastybento.bskyblock.api.panels.PanelItem;

public class PanelItemBuilder {
    private ItemStack icon;
    private String name;
    private String description;
    private boolean glow;
    private PanelItem.ClickHandler clickHandler;

    public PanelItemBuilder setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public PanelItemBuilder setName(String name) {
         this.name = name;
         return this;
    }

    public PanelItemBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public PanelItemBuilder setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public PanelItemBuilder setClickHandler(PanelItem.ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public PanelItem build() {
        return new PanelItem(icon, name, description, glow, clickHandler);
    }
}
