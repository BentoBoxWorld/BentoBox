package world.bentobox.bentobox.api.panels;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Represents a tabbed panel. The top row of the panel is made up of up to 9 icons that relate to tabs.
 * Only the active tab is shown.
 * @author tastybento
 *
 */
public class TabbedPanel implements PanelListener {

    private final TabbedPanelBuilder tpb;
    private @NonNull BentoBox plugin = BentoBox.getInstance();
    private int activeTab;

    public TabbedPanel(TabbedPanelBuilder tpb) {
        this.tpb = tpb;
    }

    /**
     * @param activeTab - the tab to show referenced by the slot (0 through 8)
     * @param page - the page of the tab to show (if multi paged)
     */
    public void openPanel(int activeTab, int page) {
        this.activeTab = activeTab;
        PanelBuilder panelBuilder = new PanelBuilder().listener(this);
        // Set title
        panelBuilder.name(tpb.getTabs().get(activeTab).getName());

        // Set up the tabbed header
        setupHeader(panelBuilder);

        // Show the active tab
        if (tpb.getTabs().containsKey(activeTab)) {
            List<PanelItem> items = tpb.getTabs().get(activeTab).getPanelItems();
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
     * @param user - viewer
     * @param tab - active tab
     */
    private void setupHeader(PanelBuilder panelBuilder) {
        // Set up top
        for (int i = 0; i < 9; i++) {
            panelBuilder.item(i, new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).build());
        }
        // Add icons
        for (Entry<Integer, Tab> tabPanel : tpb.getTabs().entrySet()) {
            // Set the glow of the active tab
            tabPanel.getValue().getIcon().setGlow(tabPanel.getKey() == activeTab);
            // Add the icon to the top row
            panelBuilder.item(tabPanel.getKey(), tabPanel.getValue().getIcon());
        }

    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Trap tab clicks else pass to tab
        if (event.isLeftClick() && tpb.getTabs().containsKey(event.getRawSlot())) {
            event.setCancelled(true);
            this.openPanel(event.getRawSlot(), 0);
        }
    }

}
