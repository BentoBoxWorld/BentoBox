package world.bentobox.bentobox.api.panels.builders;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.World;

import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;

/**
 * Builds {@link TabbedPanel}'s
 * @author tastybento
 * @since 1.6.0
 */
public class TabbedPanelBuilder {

    private int size;
    private final Map<Integer, Tab> tabs = new TreeMap<>();
    private int startingSlot;
    private World world;
    private User user;
    private boolean hideIfEmpty;

    /**
     * Forces panel to be a specific number of slots.
     * @param size - size to be
     * @return PanelBuilder - PanelBuilder
     */
    public TabbedPanelBuilder size(int size) {
        this.size = size;
        return this;
    }

    /**
     * Sets the user who will get this panel. This will open it immediately when it is built
     * @param user - the User
     * @return PanelBuilder
     */
    public TabbedPanelBuilder user(User user) {
        this.user = user;
        return this;
    }

    /**
     * @param world - world that applies to this tab
     * @return TabbedPanelBuilder
     */
    public TabbedPanelBuilder world(World world) {
        this.world = world;
        return this;
    }

    /**
     * Add a tab to the panel
     * @param slot - slot of panel (0 to 9)
     * @param tab - tab to show
     * @return TabbedPanelBuilder
     */
    public TabbedPanelBuilder tab(int slot, Tab tab) {
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
    public TabbedPanelBuilder startingSlot(int slot) {
        if (slot < 0 || slot > 9) {
            throw new InvalidParameterException("Slot must be between 0 and 9");
        }
        startingSlot = slot;
        return this;
    }

    /**
     * Hides the panel from view if there are no panel items in it
     * @return TabbedPanelBuilder
     */
    public TabbedPanelBuilder hideIfEmpty() {
        this.hideIfEmpty = true;
        return this;
    }
    /**
     * Build the panel
     * @return Panel
     */
    public TabbedPanel build() {
        // Set starting slot
        if (!tabs.isEmpty() && !tabs.containsKey(startingSlot)) {
            startingSlot = ((TreeMap<Integer, Tab>)tabs).firstKey();
        }
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

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the hideIfEmpty
     */
    public boolean isHideIfEmpty() {
        return hideIfEmpty;
    }



}
