package us.tastybento.bskyblock.api.panels.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private boolean playerHead;

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
    public PanelItemBuilder icon(String playerName) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        this.icon = item;
        this.name = playerName;
        this.playerHead = true;
        return this;
    }


    public PanelItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Adds a list of strings to the descriptions
     * @param description - List of strings
     * @return PanelItemBuilder
     */
    public PanelItemBuilder description(List<String> description) {
        this.description.addAll(description);
        return this;
    }

    /**
     * Add any number of lines to the description
     * @param description strings of lines
     * @return PanelItemBuilder
     */
    public PanelItemBuilder description(String... description) {
        List<String> additions = Arrays.asList(description);
        ArrayList<String> updatableList = new ArrayList<>();
        updatableList.addAll(this.description);
        updatableList.addAll(additions);
        this.description = updatableList;
        return this;
    }

    /**
     * Adds a line to the description
     * @param description - string
     * @return PanelItemBuilder
     */
    public PanelItemBuilder description(String description) {
        for (String line : description.split("\n")) {
            this.description.add(line);
        }
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
        return new PanelItem(icon, name, description, glow, clickHandler, playerHead);
    }

}
