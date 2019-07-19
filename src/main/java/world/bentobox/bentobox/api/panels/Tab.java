package world.bentobox.bentobox.api.panels;

import org.bukkit.World;

import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Represents a tab in a Tabbed Panel. Contains panel items.
 *
 * @author tastybento
 *
 */
public interface Tab {

    // The icon that should be shown at the top of the tabbed panel
    PanelItem getIcon();

    /**
     * Return the panel items for this tab
     * @param user
     * @param world
     * @param page
     * @param panelBuilder
     */
    void returnPanelItems(User user, World world, int page, PanelBuilder panelBuilder);

    /**
     * Set user who is viewing - used for translations
     * @param user
     */
    void setUser(User user);

    /**
     * Set friendly name of world - substitutes placeholders
     * @param friendlyWorldName
     */
    void setFriendlyWorldName(String friendlyWorldName);


}
