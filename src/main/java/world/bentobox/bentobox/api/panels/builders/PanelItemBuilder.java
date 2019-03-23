package world.bentobox.bentobox.api.panels.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;

public class PanelItemBuilder {
    private ItemStack icon = new ItemStack(Material.AIR);
    private String name = "";
    private List<String> description = new ArrayList<>();
    private boolean glow = false;
    private PanelItem.ClickHandler clickHandler;
    private boolean playerHead;
    private boolean invisible;

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
     * @param playerName - player's name
     * @return PanelItemBuilder
     */
    public PanelItemBuilder icon(String playerName) {
        this.icon = new ItemStack(Material.PLAYER_HEAD, 1);
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
        Collections.addAll(this.description, description.split("\n"));
        return this;
    }

    public PanelItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public PanelItemBuilder invisible(boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    public PanelItemBuilder clickHandler(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public PanelItem build() {
        return new PanelItem(this);
    }

    /**
     * @return the icon
     */
    public ItemStack getIcon() {
        return icon;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public List<String> getDescription() {
        return description;
    }

    /**
     * @return the glow
     */
    public boolean isGlow() {
        return glow;
    }

    /**
     * @return the clickHandler
     */
    public PanelItem.ClickHandler getClickHandler() {
        return clickHandler;
    }

    /**
     * @return the playerHead
     */
    public boolean isPlayerHead() {
        return playerHead;
    }

    /**
     * @return the invisible
     */
    public boolean isInvisible() {
        return invisible;
    }

}
