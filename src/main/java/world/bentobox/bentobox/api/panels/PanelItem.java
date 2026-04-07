package world.bentobox.bentobox.api.panels;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Represents an item in a {@link Panel}
 * @author tastybento
 *
 */
public class PanelItem {

    /**
     * @return an empty PanelItem
     */
    public static PanelItem empty() {
        return new PanelItemBuilder().build();
    }

    private ItemStack icon;
    private ClickHandler clickHandler;
    private List<String> description;
    private String name;
    private boolean glow;
    private ItemMeta meta;
    private final String playerHeadName;
    private boolean invisible;

    public PanelItem(PanelItemBuilder builtItem) {
        this.icon = builtItem.getIcon();
        this.icon.setAmount(builtItem.getAmount());
        this.playerHeadName = builtItem.getPlayerHeadName();
        // Get the meta
        meta = icon.getItemMeta();
        if (meta != null) {
            // Set flags to neaten up the view
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            icon.setItemMeta(meta);
        }

        this.clickHandler = builtItem.getClickHandler();

        // Create the final item
        setName(builtItem.getName());
        setDescription(builtItem.getDescription());
        setGlow(builtItem.isGlow());
        setInvisible(builtItem.isInvisible());

    }

    /**
     * @return the icon itemstack
     */
    public ItemStack getItem() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
        if (meta != null) {
            meta.lore(description.stream()
                    .map(Util::parseMiniMessageOrLegacy)
                    .map(PanelItem::removeDefaultItalic)
                    .toList());
            icon.setItemMeta(meta);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (meta != null) {
            meta.displayName(name != null ? removeDefaultItalic(Util.parseMiniMessageOrLegacy(name)) : null);
            icon.setItemMeta(meta);
        }
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        if (meta != null && !inTest()) {
            if (invisible) {
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.VANISHING_CURSE);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
        }
    }

    public Optional<ClickHandler> getClickHandler() {
        return Optional.ofNullable(clickHandler);
    }

    /**
     * @param clickHandler the clickHandler to set
     * @since 1.6.0
     */
    public void setClickHandler(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
        if (inTest()) {
            return;
        }
        if (meta != null) {
            try {
                meta.setEnchantmentGlintOverride(glow);
            } catch (NoSuchMethodError e) {
                // Try the old way
                if (meta != null) {
                    if (glow) {
                        meta.addEnchant(Enchantment.LURE, 0, true);
                    } else {
                        meta.removeEnchant(Enchantment.LURE);
                    }
                    icon.setItemMeta(meta);
                }
            }
            icon.setItemMeta(meta);

        }
    }

    /**
     * This checks the stack trace for @Test to determine if a test is calling the code and skips.
     * TODO: when we find a way to mock Enchantment, remove this.
     * @return true if it's a test.
     */
    private boolean inTest() {
        return Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(e -> e.getClassName().endsWith("Test"));
    }

    /**
     * @return the playerHead
     */
    public boolean isPlayerHead() {
        return playerHeadName != null && !playerHeadName.isEmpty();
    }

    /**
     * @return the playerHeadName
     * @since 1.9.0
     */
    public String getPlayerHeadName() {
        return playerHeadName;
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

    /**
     * Removes the default italic styling that Minecraft applies to item display names and lore.
     * If the component does not explicitly set italic, this method sets it to false to prevent
     * the Minecraft client from applying its default italic rendering.
     *
     * @param component the component to process
     * @return the component with italic explicitly disabled if it was not already set
     */
    private static Component removeDefaultItalic(Component component) {
        if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            return component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
        return component;
    }

    public void setHead(ItemStack itemStack) {
        // update amount before replacing.
        itemStack.setAmount(this.icon.getAmount());
        ItemMeta originalMeta = this.icon.getItemMeta();

        this.icon = itemStack;

        // Get the meta
        meta = icon.getItemMeta();
        if (meta != null) {
            // Set flags to neaten up the view
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            if (originalMeta != null && originalMeta.hasCustomModelDataComponent()) {
                meta.setCustomModelDataComponent(originalMeta.getCustomModelDataComponent());
            }

            icon.setItemMeta(meta);
        }
        // Create the final item
        setName(name);
        setDescription(description);
        setGlow(glow);
    }
}
