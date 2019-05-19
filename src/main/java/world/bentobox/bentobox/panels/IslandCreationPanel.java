package world.bentobox.bentobox.panels;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * Displays the available BlueprintBundles to pick up as the island.
 * @author tastybento
 * @since 1.5.0
 */
public class IslandCreationPanel {

    private IslandCreationPanel() {}

    /**
     * Shows a player a panel of selectable blueprint bundles. Checks user's permission
     * @param command - the command requesting the panel, e.g., create or reset
     * @param user - the user
     * @param label - label
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user, @NonNull String label) {
        BentoBox plugin = BentoBox.getInstance();
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(user.getTranslation("commands.island.create.pick")).user(user);
        // Get the bundles
        Collection<BlueprintBundle> bbs = plugin.getBlueprintsManager().getBlueprintBundles((@NonNull GameModeAddon) command.getAddon()).values();
        // Loop through them and create items in the panel
        for (BlueprintBundle bb : bbs) {
            String perm = command.getPermissionPrefix() + "island.create." + bb.getUniqueId();
            if (bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)
                    || !bb.isRequirePermission()
                    || user.hasPermission(perm)) {
                // Add an item
                pb.item(new PanelItemBuilder().name(bb.getDisplayName()).description(bb.getDescription())
                        .icon(bb.getIcon()).clickHandler((panel, user1, clickType, slot1) -> {
                            user1.closeInventory();
                            command.execute(user1, label, Collections.singletonList(bb.getUniqueId()));
                            return true;
                        }).build());
            }
        }
        pb.build();
    }
}
