package us.tastybento.bskyblock.api.panels;

import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;

public class PanelItem {

    public static PanelItem empty() {
        return new PanelItemBuilder().build();
    }

    private ItemStack icon;
    private Optional<ClickHandler> clickHandler;
    private List<String> description;
    private String name;
    private boolean glow;
    private ItemMeta meta;

    public PanelItem(ItemStack icon, String name, List<String> description, boolean glow, Optional<ClickHandler> clickHandler) {
        this.icon = icon;
        // Get the meta
        meta = icon.getItemMeta();

        this.clickHandler = clickHandler;
        
        // Create the final item
        this.setName(name);
        this.setDescription(description);
        this.setGlow(glow);

        // Set flags to neaten up the view
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        icon.setItemMeta(meta);
    }

    public ItemStack getItem() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
        meta.setLore(description);
        icon.setItemMeta(meta);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        meta.setDisplayName(name);
        icon.setItemMeta(meta);
    }

    public Optional<ClickHandler> getClickHandler() {
        return clickHandler;
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
        if (glow)
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 0, true);
        else
            meta.addEnchant(Enchantment.ARROW_DAMAGE, 0, false);
    }

    /**
     * Click handler interface
     *
     */
    public interface ClickHandler {
        /**
         * This is executed when the icon is clicked
         * @param user
         * @param click
         * @return true if the click event should be cancelled
         */
        boolean onClick(User user, ClickType click);
    }
}
