package world.bentobox.bentobox.api.panels;

import java.util.List;

/**
 * Represents a tab in a {@link TabbedPanel}. Contains {@link PanelItem}'s.
 *
 * @author tastybento
 * @since 1.6.0
 *
 */
public interface Tab {

    // The icon that should be shown at the top of the tabbed panel
    PanelItem getIcon();

    /**
     * @return the name of this tab
     */
    String getName();

    /**
     * Return the panel items for this tab
     * @return a list of items in slot order
     */
    List<PanelItem> getPanelItems();

    /**
     * @return the permission required to view this tab or empty if no permission required
     */
    String getPermission();

}
