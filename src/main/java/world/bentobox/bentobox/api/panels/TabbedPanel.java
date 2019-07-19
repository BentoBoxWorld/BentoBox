package world.bentobox.bentobox.api.panels;

import java.security.InvalidParameterException;
import java.util.Map.Entry;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;

public class TabbedPanel extends Panel {

    //
    private final TabbedPanelBuilder tpb;

    public TabbedPanel(TabbedPanelBuilder tpb) {
        // Do not give the user yet because we don't want the panel to open
        super(tpb.getName(), tpb.getItems(), tpb.getSize(), null, tpb.getListener());
        this.tpb = tpb;

    }

    /**
     * @param plugin - plugin
     * @param user - user who is seeting the panel
     * @param world - the game world this panel is for (may be different from the user)
     * @param tab - the tab to show referenced by the slot
     * @param page - the page of the tab to show
     */
    public void openPanel(@NonNull BentoBox plugin, @NonNull User user, @NonNull World world, int tab, int page) {
        String friendlyWorldName = plugin.getIWM().getFriendlyName(world);
        // Create the panel
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation(tpb.getName(), "[world_name]", friendlyWorldName))
                .size(54);

        setupHeader(user, panelBuilder, tab, world, friendlyWorldName);

        // Show the tab
        if (tpb.getTabs().containsKey(tab)) {
            tpb.getTabs().get(tab).returnPanelItems(user, world, page, panelBuilder);
        } else {
            throw new InvalidParameterException("Unknown tab slot number " + tab);
        }

        // Show it to the player
        panelBuilder.build().open(user);

    }

    private void setupHeader(User user, PanelBuilder panelBuilder, int tab, World world, String friendlyWorldName) {
        for (Entry<Integer, Tab> tabPanel : tpb.getTabs().entrySet()) {
            // Set the glow of the active tab
            tabPanel.getValue().getIcon().setGlow(tabPanel.getKey() == tab);
            // Set the user and the friendlyWorldName
            tabPanel.getValue().setUser(user);
            tabPanel.getValue().setFriendlyWorldName(friendlyWorldName);
            // Add the icon to the top row
            panelBuilder.item(tabPanel.getKey(), tabPanel.getValue().getIcon());
        }

    }

}
