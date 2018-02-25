package us.tastybento.bskyblock.panels;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;

/**
 * @author Poslovitch
 */
public class SettingsPanel {

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(User user) {
        PanelBuilder panelBuilder = new PanelBuilder()
                .setName(user.getTranslation("panels.settings.title"));

        for (Flag flag : BSkyBlock.getInstance().getFlagsManager().getFlags()) {
            PanelItem flagIcon = flag.toPanelItem(user);
            panelBuilder.addItem(flagIcon);
        }

        panelBuilder.build().open(user);
    }


}
