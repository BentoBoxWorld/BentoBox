package world.bentobox.bentobox.api.panels;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
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
            throw new InvalidParameterException("Attemot to open a non-existent tab in a tabbed panel. Missing tab #" + activeTab);
        }
        if (page < 0) {
            // Request to open a non-existent tab
            throw new InvalidParameterException("Attemot to open a tab in a tabbed panel to a negative page! " + page);
        }
        this.activeTab = activeTab;
        this.activePage = page;
        PanelBuilder panelBuilder = new PanelBuilder().listener(this).size(tpb.getSize());
        // Get the tab
        Tab tab = tpb.getTabs().get(activeTab);

        // Set title
        panelBuilder.name(tab.getName());

        // Set up the tabbed header
        setupHeader(panelBuilder);

        // Show the active tab
        if (tpb.getTabs().containsKey(activeTab)) {
            List<PanelItem> items = tab.getPanelItems();
            items.stream().skip(page * 43L).limit(page * 43L + 43L).forEach(panelBuilder::item);
            // Add forward and backward icons
            if (page > 0) {
                // Previous page icon
                panelBuilder.item(new PanelItemBuilder().icon(Material.ARROW).name(tpb.getUser().getTranslation("previous")).clickHandler((panel, user1, clickType, slot1) -> {
                    openPanel(activeTab, page - 1);
                    return true;
                }).build());
            }
            if ((page + 1) * 44 < items.size()) {
                // Next page icon
                panelBuilder.item(new PanelItemBuilder().icon(Material.ARROW).name(tpb.getUser().getTranslation("next")).clickHandler((panel, user1, clickType, slot1) -> {
                    openPanel(activeTab, page + 1);
                    return true;
                }).build());
            }
        } else {
            throw new InvalidParameterException("Unknown tab slot number " + activeTab);
        }
        // Show it to the player
        panelBuilder.build().open(tpb.getUser());
    }

    /**
     * Shows the top row of icons
     * @param panelBuilder - panel builder
     */
    private void setupHeader(PanelBuilder panelBuilder) {
        // Set up top
        for (int i = 0; i < 9; i++) {
            panelBuilder.item(i, new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name("").build());
        }
        // Add icons
        for (Entry<Integer, Tab> tabPanel : tpb.getTabs().entrySet()) {
            // Set the glow of the active tab
            tabPanel.getValue().getIcon().setGlow(tabPanel.getKey() == activeTab);
            // Add the icon to the top row
            if (tabPanel.getValue().getPermission().isEmpty() || tpb.getUser().hasPermission(tabPanel.getValue().getPermission()) || tpb.getUser().isOp()) {
                panelBuilder.item(tabPanel.getKey(), tabPanel.getValue().getIcon());
            }
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

}
