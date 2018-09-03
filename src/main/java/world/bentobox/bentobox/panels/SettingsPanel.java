package world.bentobox.bentobox.panels;

import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Creates settings panels
 * @author Poslovitch, tastybento
 */
public class SettingsPanel {

    private static final String PROTECTION_PANEL = "protection.panel.";

    private SettingsPanel() {}

    /**
     * Dynamically creates the panel.
     * @param plugin - plugin
     * @param user - user to show panel to
     * @param flagType - initial view
     * @param world - world
     */
    public static void openPanel(BentoBox plugin, User user, Flag.Type flagType, World world) {
        String friendlyWorldName = plugin.getIWM().getFriendlyName(world);
        // Create the panel
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".title", "[world_name]", friendlyWorldName))
                .size(54);

        setupHeader(user, panelBuilder, flagType, world, friendlyWorldName);

        plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(flagType))
        .sorted(Comparator.comparing(Flag::getID)).forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));

        // Show it to the player
        panelBuilder.build().open(user);
    }

    private static void setupHeader(User user, PanelBuilder panelBuilder, Flag.Type currentFlagType, World world, String friendlyWorldName) {
        int slot = 2;
        for (Flag.Type flagType : Flag.Type.values()) {
            PanelItem panelItem = new PanelItemBuilder()
                    .icon(flagType.getIcon())
                    .name(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".title", "[world_name]", friendlyWorldName))
                    .description(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".description"))
                    .glow(flagType.equals(currentFlagType))
                    .clickHandler((panel, user1, clickType, slot1) -> {
                        if (!flagType.equals(currentFlagType)) {
                            openPanel(BentoBox.getInstance(), user, flagType, world);
                        }
                        return true;
                    })
                    .build();
            panelBuilder.item(slot, panelItem);
            slot += 2;
        }

        for (int i = 0; i < 9; i++) {
            if (!panelBuilder.slotOccupied(i)) {
                panelBuilder.item(i, new PanelItemBuilder().icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build());
            }
        }
    }
}
