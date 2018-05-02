package us.tastybento.bskyblock.panels;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
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

        // Make the help item
        PanelItem help = new PanelItemBuilder()
                .name(user.getTranslation("protection.panel.help-item.name"))
                .icon("MHF_Question") // Question marks
                .build();
        // Place it at position 8 (end of first row)
        panelBuilder.item(8, help);

        // Add flags after position 8, i.e., from second row
        plugin.getFlagsManager().getFlags().forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));

        // Make the panel
        panelBuilder.build().open(user);
    }
}
