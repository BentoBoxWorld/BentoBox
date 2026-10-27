package world.bentobox.bentobox.listeners;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.InventoryView;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class PanelListenerManager implements Listener {

    // Folia fires inventory events on each player's region thread; a lost entry here means clicks
    // are no longer cancelled and items can be taken out of the panel.
    private static final Map<UUID, Panel> openPanels = new ConcurrentHashMap<>();

    /**
     * Actions that, when performed on a slot of the player's own inventory, only move items
     * within that inventory, to the cursor, or out of the world via a drop. None of them can put
     * an item into the panel. Shift-click ({@link InventoryAction#MOVE_TO_OTHER_INVENTORY}) and
     * double-click ({@link InventoryAction#COLLECT_TO_CURSOR}, which also gathers matching panel
     * icons) are deliberately absent.
     */
    private static final EnumSet<InventoryAction> PLAYER_INVENTORY_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT, InventoryAction.HOTBAR_SWAP,
            InventoryAction.PICKUP_FROM_BUNDLE, InventoryAction.PICKUP_ALL_INTO_BUNDLE,
            InventoryAction.PICKUP_SOME_INTO_BUNDLE, InventoryAction.PLACE_FROM_BUNDLE,
            InventoryAction.PLACE_ALL_INTO_BUNDLE, InventoryAction.PLACE_SOME_INTO_BUNDLE);

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        User user = User.getInstance(event.getWhoClicked()); // The player that clicked the item
        InventoryView view = event.getView();

        // Open the inventory panel that this player has open (they can only ever have one)
        Panel openPanel = openPanels.get(user.getUniqueId());
        if (openPanel != null && openPanel.getInventory().equals(event.getInventory())) {
            // Cancel the event. If they don't want it to be cancelled then the click handler(s) should
            // uncancel it. If gui was from our environment, then cancel event anyway.
            event.setCancelled(true);

            // Get the panel itself
            Panel panel = openPanel;

            // Let the player use their own inventory while the panel is open, unless the panel
            // reads clicks there (e.g. "click an item in your inventory" icon pickers).
            if (isPlayerInventoryClickAllowed(panel, event)) {
                event.setCancelled(false);
                return;
            }

            // Apply the click cooldown for throttled panels (tabbed and settings panels) first,
            // before any other (potentially expensive) work. A rejected spam click must do the
            // absolute minimum: without this early return, every click - even the throttled ones -
            // would still pay for a view.getTitle() render and a MiniMessage title parse below,
            // which is what keeps MSPT high while a settings GUI is being spam-clicked.
            // Whether the clicked slot can actually do anything is passed along so that when a
            // client sends several click packets for one physical click, the cooldown keeps the
            // one that lands on a button rather than whichever arrived first (#3049).
            Optional<PanelListener> throttled = panel.getListener().filter(PanelListener::hasClickCooldown);
            int topSize = event.getView().getTopInventory().getSize();
            int rawSlot = event.getRawSlot();
            // Only slots inside the panel can be actionable; the player's own inventory never is
            if (throttled.isPresent() && BentoBox.getInstance().onTimeout(user, panel,
                    rawSlot >= 0 && rawSlot < topSize && throttled.get().isActionableSlot(rawSlot))) {
                return;
            }

            // Check the name of the panel using plain text comparison.
            // Panel names may contain MiniMessage tags or legacy § codes; view.getTitle() returns
            // rendered text. The panel's plain name is cached, so this does not re-parse the title.
            String viewTitle = Util.stripColor(view.getTitle());
            String panelName = panel.getPlainName();
            if (viewTitle.equals(panelName)) {
                // Close inventory if clicked outside and if setting is true
                if (BentoBox.getInstance().getSettings().isClosePanelOnClickOutside() && event.getSlotType().equals(SlotType.OUTSIDE)) {
                    event.getWhoClicked().closeInventory();
                    return;
                }

                // Check that they clicked on a specific item. Raw slots beyond the top inventory
                // belong to the player's own inventory and must never hit a panel handler.
                boolean inTop = rawSlot >= 0 && rawSlot < topSize;
                PanelItem pi = inTop ? panel.getItems().get(rawSlot) : null;
                if (inTop) {
                    // A panel icon must never be taken out, whatever a handler returns
                    event.setCancelled(true);
                }
                if (pi != null) {
                    // The return value only says "handled"; the click is already cancelled above
                    pi.getClickHandler().ifPresent(handler ->
                            handler.onClick(panel, user, event.getClick(), event.getSlot()));
                }
                // If there is a listener, then run it and refresh the panel
                panel.getListener().ifPresent(l -> {
                    l.onInventoryClick(user, event);
                    // Refresh
                    l.refreshPanel();
                });

            } else {
                // Wrong name - delete this panel
                openPanels.remove(user.getUniqueId());
                // Notify the listener before closing: once the panel is unregistered the close
                // event cannot find it any more, which would leak the panel's deferred saves.
                panel.getListener().ifPresent(l -> l.onInventoryClose(new InventoryCloseEvent(view)));
                // This avoids GUIs being left open so that items can be taken.
                user.closeInventory();
            }
        }
    }

    /**
     * Whether a click can be left to vanilla: it is in the player's own (bottom) inventory, it
     * cannot move an item into the panel, and the panel does not want to see it.
     */
    private boolean isPlayerInventoryClickAllowed(Panel panel, InventoryClickEvent event) {
        return event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getBottomInventory())
                && PLAYER_INVENTORY_ACTIONS.contains(event.getAction())
                && panel.getListener().map(PanelListener::allowsPlayerInventoryClicks).orElse(true);
    }

    /**
     * Stops items being dragged into a panel. Panels never accept items, and anything that got
     * into one would be lost when it closes. Drags confined to the player's own inventory are fine.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent event) {
        Panel panel = openPanels.get(event.getWhoClicked().getUniqueId());
        if (panel != null && panel.getInventory().equals(event.getInventory())) {
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        Panel panel = openPanels.get(event.getPlayer().getUniqueId());
        if (panel != null) {
            // Run any close inventory methods
            panel.getListener().ifPresent(l -> l.onInventoryClose(event));
            openPanels.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLogOut(PlayerQuitEvent event) {
        // Paper may not fire InventoryCloseEvent before the quit event. Notify the panel anyway,
        // otherwise SettingsPanel's beginDeferSaves() is never balanced and that island stops
        // saving flag changes until the server restarts.
        Panel panel = openPanels.remove(event.getPlayer().getUniqueId());
        if (panel != null) {
            panel.getListener().ifPresent(l -> l.onInventoryClose(
                    new InventoryCloseEvent(event.getPlayer().getOpenInventory())));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("BentoBox")) {
            closeAllPanels();
        }
    }

    /**
     * Closes all open BentoBox panels
     * @since 1.5.0
     */
    public static void closeAllPanels() {
        // Use stream clones to avoid concurrent modification exceptions
        new ArrayList<>(openPanels.values()).forEach(p ->
        new ArrayList<>(p.getInventory().getViewers()).forEach(HumanEntity::closeInventory));
    }

    /**
     * @return the openPanels
     */
    public static Map<UUID, Panel> getOpenPanels() {
        return openPanels;
    }

}
