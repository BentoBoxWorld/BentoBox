package world.bentobox.bentobox.api.panels;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import world.bentobox.bentobox.api.user.User;

/**
 * This will be called if registered and if a player clicks on a panel
 * @author tastybento
 *
 */
public interface PanelListener {

    /**
     * This is called when the panel is first setup
     */
    void setup();

    void onInventoryClose(InventoryCloseEvent event);

    void onInventoryClick(User user, InventoryClickEvent event);

    /**
     * Called after a user has clicked on a panel item.
     * Used to refresh the panel in its entirety
     * @since 1.6.0
     */
    default void refreshPanel() {}

    /**
     * Whether clicks on the panel this listener is attached to are subject to the panel click
     * cooldown (see {@link world.bentobox.bentobox.BentoBox#onTimeout(User, Panel, boolean)}).
     * Panels whose items are rebuilt on every click, such as the settings panels, should return
     * {@code true} so that spam-clicking cannot raise MSPT; simple panels need not.
     * @return {@code true} if the click cooldown applies to this panel
     * @since 3.23.1
     */
    default boolean hasClickCooldown() {
        return false;
    }

    /**
     * Whether a click on a raw slot of the panel can actually do any work, e.g. it lands on an
     * item with a click handler rather than on filler. Only consulted when
     * {@link #hasClickCooldown()} is {@code true}: when a client sends several click packets for
     * one physical click, the cooldown keeps the one that lands on a button rather than whichever
     * arrived first.
     * @param rawSlot raw slot that was clicked
     * @return {@code true} if a click on this slot does work; the default assumes it does
     * @since 3.23.1
     */
    default boolean isActionableSlot(int rawSlot) {
        return true;
    }
}
