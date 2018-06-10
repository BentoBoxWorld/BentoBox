package us.tastybento.bskyblock.panels;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.user.User;

/**
 * @author Poslovitch
 */
public class SettingsPanel {

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(BSkyBlock plugin, User user) {
        // Make a panel for settings
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation("protection.panel.title"));

        // Add flags, sorted
        plugin.getFlagsManager().getFlags().stream().sorted((e1, e2) -> e1.getID().compareTo(e2.getID())).forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));
        // Make the panel
        panelBuilder.build().open(user);
    }
}
