package world.bentobox.bentobox.api.panels.builders;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;

/**
 * Builds tabbed panels
 * @author tastybento
 *
 */
public class TabbedPanelBuilder extends PanelBuilder {

    private Map<Integer, Tab> tabs = new HashMap<>();
    private int startingSlot;
    private World world;

    /**
     * Add a tab to the panel
     * @param slot - slot of panel (0 to 9)
     * @param tab - tab to show
     * @return TabbedPanelBuilder
     */
    TabbedPanelBuilder tab(int slot, Tab tab) {
        if (slot < 0 || slot > 9) {
            throw new InvalidParameterException("Slot must be between 0 and 9");
        }
        tabs.put(slot, tab);
        return this;
    }

    /**
     * The default tab to show
     * @param slot - slot value between 0 and 9
     * @return TabbedPanelBuilder
     */
    TabbedPanelBuilder startingSlot(int slot) {
        if (slot < 0 || slot > 9) {
            throw new InvalidParameterException("Slot must be between 0 and 9");
        }
        startingSlot = slot;
        return this;
    }

    /**
     * Build the panel
     * @return Panel
     */
    @Override
    public Panel build() {
        // The size is fixed right now
        this.size(54);
        return new TabbedPanel(this);
    }

    /**
     * @return the tabs
     */
    public Map<Integer, Tab> getTabs() {
        return tabs;
    }

    /**
     * @return the startingSlot
     */
    public int getStartingSlot() {
        return startingSlot;
    }



}
