package us.tastybento.bskyblock.api.panels.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;

public class PanelItemBuilder {
    private ItemStack icon = new ItemStack(Material.AIR);
    private String name = "";
    private List<String> description = new ArrayList<>();
    private boolean glow = false;
    private PanelItem.ClickHandler clickHandler;

    public PanelItemBuilder icon(Material icon) {
        this.icon = new ItemStack(icon);
        return this;
    }

    public PanelItemBuilder icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public PanelItemBuilder name(String name) {
         this.name = name;
         return this;
    }

    public PanelItemBuilder description(List<String> description) {
        this.description = description;
        return this;
    }

    public PanelItemBuilder description(String... description) {
        Collections.addAll(this.description, description);
        return this;
    }

    public PanelItemBuilder description(String description) {
        this.description.add(description);
        return this;
    }
    
    public PanelItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public PanelItemBuilder clickHandler(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public PanelItem build() {
        if (icon == null)
            Bukkit.getLogger().info("DEBUG: icon is null");
        return new PanelItem(icon, name, description, glow, clickHandler);
    }

}
