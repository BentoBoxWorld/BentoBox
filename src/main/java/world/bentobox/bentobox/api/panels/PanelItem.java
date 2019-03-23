package world.bentobox.bentobox.api.panels;

import java.util.List;
import java.util.Optional;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

public class PanelItem {

    public static PanelItem empty() {
        return new PanelItemBuilder().build();
    }

    private ItemStack icon;
    private ClickHandler clickHandler;
    private List<String> description;
    private String name;
    private boolean glow;
    private ItemMeta meta;
    private boolean playerHead;
    private boolean invisible;

    public PanelItem(PanelItemBuilder builtItem) {
        this.icon = builtItem.getIcon();
        this.playerHead = builtItem.isPlayerHead();
        // Get the meta
        meta = icon.getItemMeta();
        if (meta != null) {
            // Set flags to neaten up the view
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            icon.setItemMeta(meta);
        }

        this.clickHandler = builtItem.getClickHandler();

        // Create the final item
        setName(builtItem.getName());
        setDescription(builtItem.getDescription());
        setGlow(builtItem.isGlow());
        setInvisible(builtItem.isInvisible());

    }

    public ItemStack getItem() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
        if (meta != null) {
            meta.setLore(description);
            icon.setItemMeta(meta);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLocalizedName(name); //Localized name cannot be overridden by the player using an anvils
            icon.setItemMeta(meta);
        }
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        if (meta != null) {
            if (invisible) {
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            } else {
                meta.removeEnchant(Enchantment.VANISHING_CURSE);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            }
        }
    }

    public Optional<ClickHandler> getClickHandler() {
        return Optional.ofNullable(clickHandler);
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
        if (meta != null) {
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 0, glow);
            icon.setItemMeta(meta);
        }
    }

    /**
     * @return the playerHead
     */
    public boolean isPlayerHead() {
        return playerHead;
    }

    /**
     * Click handler interface
     *
     */
    public interface ClickHandler {
        /**
         * This is executed when the icon is clicked
         * @param panel - the panel that is being clicked
         * @param user - the User
         * @param clickType - the click type
         * @param slot - the slot that was clicked
         * @return true if the click event should be cancelled
         */
        boolean onClick(Panel panel, User user, ClickType clickType, int slot);
    }

    public void setHead(ItemStack itemStack) {
        this.icon = itemStack;
        // Get the meta
        if (meta != null) {
            meta = icon.getItemMeta();
            // Set flags to neaten up the view
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            icon.setItemMeta(meta);
        }
        // Create the final item
        setName(name);
        setDescription(description);
        setGlow(glow);
    }
}
