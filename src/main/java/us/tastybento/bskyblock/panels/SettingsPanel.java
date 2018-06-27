package us.tastybento.bskyblock.panels;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;

import java.util.Comparator;

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
     * @param user the User to show the panel to
     */
    public static void openPanel(BSkyBlock plugin, User user, Flag.Type flagType) {
        // Create the panel
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".title"))
                .size(54);

        setupHeader(user, panelBuilder, flagType);

        plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(flagType))
        .sorted(Comparator.comparing(Flag::getID)).forEach((f -> panelBuilder.item(f.toPanelItem(plugin, user))));

        // Show it to the player
        panelBuilder.build().open(user);
    }

    private static void setupHeader(User user, PanelBuilder panelBuilder, Flag.Type currentFlagType) {
        int slot = 2;
        for (Flag.Type flagType : Flag.Type.values()) {
            PanelItem panelItem = new PanelItemBuilder()
                    .icon(flagType.getIcon())
                    .name(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".title"))
                    .description(user.getTranslation(PROTECTION_PANEL + flagType.toString() + ".description"))
                    .glow(flagType.equals(currentFlagType))
                    .clickHandler((panel, user1, clickType, slot1) -> {
                        if (!flagType.equals(currentFlagType)) {
                            openPanel(BSkyBlock.getInstance(), user, flagType);
                        }
                        return true;
                    })
                    .build();
            panelBuilder.item(slot, panelItem);
            slot += 2;
        }

        while(panelBuilder.nextSlot() < 9) {
            panelBuilder.item(new PanelItemBuilder().icon(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)).build());
        }
    }
}
