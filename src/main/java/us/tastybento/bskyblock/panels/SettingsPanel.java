package us.tastybento.bskyblock.panels;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.user.User;

/**
 * Creates settings panels
 * @author Poslovitch, tastybento
 */
public class SettingsPanel {

    /**
     * Dynamically creates the panel.
     * @param plugin - plugin
     * @param user the User to show the panel to
     */
    public static void openPanel(BSkyBlock plugin, User user) {
        // Make a panel for settings
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation("protection.panel.title"));

        // Add flags, sorted
        plugin.getFlagsManager().getFlags().stream().filter(f -> !f.getType().equals(Flag.Type.WORLD_SETTING))
        .sorted((e1, e2) -> e1.getID().compareTo(e2.getID())).forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));
        // Make the panel
        panelBuilder.build().open(user);
    }
    
    /**
     * Dynamically creates the world settings panel.
     * @param plugin - plugin
     * @param user the User to show the panel to
     */
    public static void openWorldSettingsPanel(BSkyBlock plugin, User user) {
        // Make a panel for settings
        PanelBuilder panelBuilder = new PanelBuilder().name(user.getTranslation("protection.panel.world-settings", "[world_name]", plugin.getIWM().getWorldName(user.getWorld())));       
        // Add flags, sorted
        plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(Flag.Type.WORLD_SETTING))
        .sorted((e1, e2) -> e1.getID().compareTo(e2.getID())).forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));
        // Make the panel
        panelBuilder.build().open(user);
    }
}
