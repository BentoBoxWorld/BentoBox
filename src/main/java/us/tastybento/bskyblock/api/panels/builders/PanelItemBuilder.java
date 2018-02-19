package us.tastybento.bskyblock.api.panels.builders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
    
    /**
     * Set icon to player's head
     * @param playerUUID - player's UUID
     * @return PanelItemBuilder
     */
    public PanelItemBuilder icon(UUID playerUUID) {
        return icon(Bukkit.getOfflinePlayer(playerUUID).getName());
    }
    
    /**
     * Set icon to player's head
     * @param playerName - player's name
     * @return PanelItemBuilder
     */
    @SuppressWarnings("deprecation")
    public PanelItemBuilder icon(String playerName) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM,1,(short)3);
        SkullMeta meta = (SkullMeta)item.getItemMeta();
        // This is deprecated, but apparently the only way to make it work right now
        meta.setOwner(playerName);
        item.setItemMeta( meta );
        this.icon = item;
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
        return new PanelItem(icon, name, description, glow, clickHandler);
    }

}
