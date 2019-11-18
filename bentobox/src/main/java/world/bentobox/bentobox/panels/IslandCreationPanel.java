package world.bentobox.bentobox.panels;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
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
        Comparator<BlueprintBundle> sortByDisplayName = (p, o) -> p.getDisplayName().compareToIgnoreCase(o.getDisplayName());
        List<BlueprintBundle> bbs = plugin.getBlueprintsManager().getBlueprintBundles(command.getAddon()).values()
                .stream().sorted(sortByDisplayName).collect(Collectors.toList());
        // Loop through them and create items in the panel
        for (BlueprintBundle bb : bbs) {
            String perm = command.getPermissionPrefix() + "island.create." + bb.getUniqueId();
            if (bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)
                    || !bb.isRequirePermission()
                    || user.hasPermission(perm)) {
                // Add an item
                PanelItem item = new PanelItemBuilder()
                        .name(bb.getDisplayName())
                        .description(bb.getDescription().stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).collect(Collectors.toList()))
                        .icon(bb.getIcon()).clickHandler((panel, user1, clickType, slot1) -> {
                            user1.closeInventory();
                            command.execute(user1, label, Collections.singletonList(bb.getUniqueId()));
                            return true;
                        }).build();
                // Determine slot
                if (bb.getSlot() < 0 || bb.getSlot() > BlueprintManagementPanel.MAX_BP_SLOT) {
                    bb.setSlot(0);
                }
                if (pb.slotOccupied(bb.getSlot())) {
                    int slot = getFirstAvailableSlot(pb);
                    if (slot == -1) {
                        // TODO add paging
                        plugin.logError("Too many blueprint bundles to show!");
                        pb.item(item);
                    } else {
                        pb.item(slot, item);
                    }
                } else {
                    pb.item(bb.getSlot(), item);
                }
            }
        }
        pb.build();
    }

    /**
     * @param pb - panel builder
     * @return first available slot, or -1 if none
     */
    private static int getFirstAvailableSlot(PanelBuilder pb) {
        for (int i = 0; i < BlueprintManagementPanel.MAX_BP_SLOT; i++) {
            if (!pb.slotOccupied(i)) {
                return i;
            }
        }
        return -1;
    }
}
