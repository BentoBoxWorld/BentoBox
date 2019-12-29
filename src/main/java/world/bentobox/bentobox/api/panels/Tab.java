package world.bentobox.bentobox.api.panels;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

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
    List<@Nullable PanelItem> getPanelItems();

    /**
     * @return the permission required to view this tab or empty if no permission required
     */
    String getPermission();

    /**
     * @return Map of icons to be shown in the tab row when the tab is active
     * Make sure these do not overlap any tabs that are in the tab row
     */
    default Map<Integer, PanelItem> getTabIcons() {
        return Collections.emptyMap();
    }
}
