package us.tastybento.bskyblock.api.panels.builders;

import java.util.List;
import java.util.Optional;

import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;

public class PanelItemBuilder {
    private ItemStack icon;
    private String name;
    private List<String> description;
    private boolean glow;
    private Optional<PanelItem.ClickHandler> clickHandler = Optional.empty();

    public PanelItemBuilder setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public PanelItemBuilder setName(String name) {
         this.name = name;
         return this;
    }

    public PanelItemBuilder setDescription(List<String> list) {
        this.description = list;
        return this;
    }

    public PanelItemBuilder setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public PanelItemBuilder setClickHandler(ClickHandler clickHandler) {
        this.clickHandler = Optional.of(clickHandler);
        return this;
    }

    public PanelItem build() {
        return new PanelItem(icon, name, description, glow, clickHandler);
    }

}
