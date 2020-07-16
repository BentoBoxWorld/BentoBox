package world.bentobox.bentobox.api.panels;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Represents a panel with tabs. The top row of the panel is made up of up to 9 icons that are made of {@link world.bentobox.bentobox.api.panels.Tab}s.
 * Only the active tab is shown. The panel will auto-refresh when a panel item is clicked, so panel item
 * click listeners do not have to actively update the panel. Viewers of the panel who do not have permission
 * to see a {@link world.bentobox.bentobox.api.panels.Tab} will not be shown it.
 *
 * @author tastybento
 * @since 1.6.0
 */
public class TabbedPanel extends Panel implements PanelListener {

    private static final String PROTECTION_PANEL = "protection.panel.";
    private static final long ITEMS_PER_PAGE = 36;
    private final TabbedPanelBuilder tpb;
    private @NonNull BentoBox plugin = BentoBox.getInstance();
    private int activeTab;
    private int activePage;
    private boolean closed;

    /**
     * Construct the tabbed panel
     * @param tpb - tabbed panel builder
     */
    public TabbedPanel(TabbedPanelBuilder tpb) {
        this.tpb = tpb;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.PanelListener#refreshPanel()
     */
    @Override
    public void refreshPanel() {
        if (closed) return;
        // Called when a player clicks on the panel
        openPanel(activeTab, activePage);
        // Reset the closed flag
        closed = false;
    }

    /**
     * Open the tabbed panel at the starting slot
     */
    public void openPanel() {
        openPanel(tpb.getStartingSlot(), 0);
    }

    /**
     * Open the tabbed panel
     * @param activeTab - the tab to show referenced by the slot (0 through 8)
     * @param page - the page of the tab to show (if multi paged)
     */
    public void openPanel(int activeTab, int page) {
        if (!tpb.getTabs().containsKey(activeTab)) {
            // Request to open a non-existent tab
            throw new InvalidParameterException("Attempt to open a non-existent tab in a tabbed panel. Missing tab #" + activeTab);
        }
        if (page < 0) {
            // Request to open a non-existent tab
            throw new InvalidParameterException("Attempt to open a tab in a tabbed panel to a negative page! " + page);
        }
        this.activeTab = activeTab;
        this.activePage = page;
        // The items in the panel
        TreeMap<Integer, PanelItem> items = new TreeMap<>();
        // Get the tab
        Tab tab = tpb.getTabs().get(activeTab);

        // Remove any tabs that have no items, if required
        if (tpb.isHideIfEmpty()) {
            tpb.getTabs().values().removeIf(t -> !t.equals(tab) && t.getPanelItems().stream().noneMatch(Objects::nonNull));
        }

        // Set up the tabbed header
        setupHeader(tab, items);

        // Show the active tab
        if (tpb.getTabs().containsKey(activeTab)) {
            List<PanelItem> panelItems = tab.getPanelItems();
            // Adds the flag items
            panelItems.stream().filter(Objects::nonNull).skip(page * ITEMS_PER_PAGE).limit(page * ITEMS_PER_PAGE + ITEMS_PER_PAGE).forEach(i -> items.put(items.lastKey() + 1, i));

            // set up the footer
            setupFooter(items);
            // Add forward and backward icons
            if (page > 0) {
                // Previous page icon
                items.put(46, new PanelItemBuilder().icon(Material.ARROW).name(tpb.getUser().getTranslation(PROTECTION_PANEL + "previous")).clickHandler((panel, user1, clickType, slot1) -> {
                    this.activePage--;
                    this.refreshPanel();
                    return true;
                }).build());
            }
            if ((page + 1) * ITEMS_PER_PAGE < panelItems.stream().filter(Objects::nonNull).count()) {
                // Next page icon
                items.put(52, new PanelItemBuilder().icon(Material.ARROW).name(tpb.getUser().getTranslation(PROTECTION_PANEL + "next")).clickHandler((panel, user1, clickType, slot1) -> {
                    this.activePage++;
                    this.refreshPanel();
                    return true;
                }).build());
            }
        } else {
            throw new InvalidParameterException("Unknown tab slot number " + activeTab);
        }
        // Show it to the player
        this.makePanel(tab.getName(), items, tpb.getSize(), tpb.getUser(), this);
    }

    /**
     * Shows the top row of icons
     * @param tab  - active tab
     * @param items - panel builder
     */
    private void setupHeader(Tab tab, TreeMap<Integer, PanelItem> items) {
        // Set up top
        for (int i = 0; i < 9; i++) {
            items.put(i, new PanelItemBuilder().icon(plugin.getSettings().getPanelFillerMaterial()).name(" ").build());
        }
        // Add icons
        for (Entry<Integer, Tab> tabPanel : tpb.getTabs().entrySet()) {
            // Add the icon to the top row
            if (tpb.getUser().hasPermission(tabPanel.getValue().getPermission())) {
                PanelItem activeIcon = tabPanel.getValue().getIcon();
                // Set the glow of the active tab
                activeIcon.setGlow(tabPanel.getValue().equals(tab));
                items.put(tabPanel.getKey(), activeIcon);
            }
        }
        // Add any subsidiary icons
        tab.getTabIcons().forEach(items::put);
    }

    private void setupFooter(TreeMap<Integer, PanelItem> items) {
        for (int i = 45; i < 54; i++) {
            items.put(i, new PanelItemBuilder().icon(plugin.getSettings().getPanelFillerMaterial()).name(" ").build());
        }
    }

    @Override
    public void setup() {
        // Not used
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        // This flag is set every time the inventory is closed or refreshed (closed and opened)
        closed = true;
    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Trap top row tab clicks
        if (event.isLeftClick() && tpb.getTabs().containsKey(event.getRawSlot())
                && (tpb.getTabs().get(event.getRawSlot()).getPermission().isEmpty()
                        || tpb.getUser().hasPermission(tpb.getTabs().get(event.getRawSlot()).getPermission()) || tpb.getUser().isOp())) {
            event.setCancelled(true);
            this.openPanel(event.getRawSlot(), 0);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
            // Reset the closed flag
            closed = false;
        }
    }

    /**
     * @return the active tab being shown to the user
     */
    public Tab getActiveTab() {
        return tpb.getTabs().get(activeTab);
    }

    /**
     * @return the activePage
     */
    public int getActivePage() {
        return activePage;
    }

    /**
     * @param activePage the activePage to set
     */
    public void setActivePage(int activePage) {
        this.activePage = activePage;
    }

    /**
     * @param activeTab the activeTab to set
     */
    public void setActiveTab(int activeTab) {
        this.activeTab = activeTab;
    }
}
