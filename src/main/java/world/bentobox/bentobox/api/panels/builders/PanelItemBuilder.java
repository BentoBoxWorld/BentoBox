package world.bentobox.bentobox.api.panels.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;

public class PanelItemBuilder {
    private ItemStack icon = new ItemStack(Material.AIR);
    private @Nullable String name = "";
    private List<String> description = new ArrayList<>();
    private boolean glow = false;
    private PanelItem.ClickHandler clickHandler;
    private String playerHeadName;
    private boolean invisible;

    /**
     * Allows to define amount of elements in stack.
     * Note: it uses item.setAmount, so it cannot overwrite minimal and maximal values.
     * f.e. Eggs will never be more than 16, and 0 will mean empty icon.
     */
    private int amount = 1;

    /**
     * Default icon if someone gives invalid material or item stack.
     */
    private static final ItemStack DEFAULT_ICON = new ItemStack(Material.PAPER);


    public PanelItemBuilder icon(@Nullable Material icon) {
        this.icon = icon == null ? DEFAULT_ICON.clone() : new ItemStack(icon);
        return this;
    }

    public PanelItemBuilder icon(@Nullable ItemStack icon) {
        this.icon = icon == null ? DEFAULT_ICON.clone() : icon;
        // use icon stack amount.
        this.amount = this.icon.getAmount();
        return this;
    }

    /**
     * Set icon to player's head
     * @param playerName - player's name
     * @return PanelItemBuilder
     */
    public PanelItemBuilder icon(String playerName) {
        this.icon = new ItemStack(Material.PLAYER_HEAD, 1);
        this.playerHeadName = playerName;
        return this;
    }

    public PanelItemBuilder name(@Nullable String name) {
        this.name = name != null ? ChatColor.translateAlternateColorCodes('&', name) : null;
        return this;
    }

    /**
     * Sets amount of items in stack.
     * @param amount new amount of items.
     * @return PanelItemBuilder
     */
    public PanelItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Adds a list of strings to the descriptions
     * @param description - List of strings
     * @return PanelItemBuilder
     */
    public PanelItemBuilder description(List<String> description) {
        description.forEach(this::description);
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
        return playerHeadName != null && !playerHeadName.isEmpty();
    }
    
    /**
     * @return the playerHead
     * @since 1.9.0
     */
    public String getPlayerHeadName() {
        return playerHeadName;
    }    

    /**
     * @return the invisible
     */
    public boolean isInvisible() {
        return invisible;
    }


    /**
     * @return amount of items in stack.
     * @since 1.13.0
     */
    public int getAmount()
    {
        return this.amount;
    }
}
