package world.bentobox.bentobox.panels;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Displays the available BlueprintBundles to pick up as the island.
 * @author Poslovitch, tastybento
 * @since 1.5.0
 */
public class IslandCreationPanel {

    private static final String LOCALE_REF = "island-creation.";

    private IslandCreationPanel() {}

    /**
     * Shows the island creation panel for this Gamemode to this User.
     * @param user the User to show the panel to.
     * @param addon the addon to display the blueprint bundles from.
     */
    public static void openPanel(@NonNull User user, @NonNull GameModeAddon addon) {
        BentoBox plugin = BentoBox.getInstance();
        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title"));

        plugin.getBlueprintsManager().getBlueprintBundles(addon).forEach((id, bundle) -> {
            PanelItemBuilder itemBuilder = new PanelItemBuilder()
                    .icon(bundle.getIcon())
                    .name(bundle.getDisplayName())
                    .description(bundle.getDescription())
                    .clickHandler((panel, user1, clickType, slot) -> {
                        user1.closeInventory();
                        // TODO create the island;
                        return true;
                    });

            builder.item(itemBuilder.build());
        });

        builder.build().open(user);
    }
}
