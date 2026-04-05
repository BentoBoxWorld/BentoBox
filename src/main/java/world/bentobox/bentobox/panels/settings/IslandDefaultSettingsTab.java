package world.bentobox.bentobox.panels.settings;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.IslandDefaultCycleClick;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Implements a {@link Tab} that enables the default island protection settings to be changed.
 * These are the protection flag rank values that new islands will receive.
 * @author tastybento
 * @since 3.2.0
 */
public class IslandDefaultSettingsTab extends SettingsTab implements Tab {

    /**
     * @param world - world
     * @param user - user
     */
    public IslandDefaultSettingsTab(World world, User user) {
        super(world, user, Type.PROTECTION);
    }

    /**
     * Get the icon for this tab
     * @return panel item
     */
    @Override
    public PanelItem getIcon() {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.icon(Material.CRACKED_STONE_BRICKS);
        pib.name(getName());
        pib.description(user.getTranslation(PROTECTION_PANEL + "ISLAND_DEFAULTS.description"));
        return pib.build();
    }

    @Override
    public String getName() {
        return user.getTranslation(PROTECTION_PANEL + "ISLAND_DEFAULTS.title", "[world_name]",
                plugin.getIWM().getFriendlyName(world));
    }

    @Override
    public String getPermission() {
        return plugin.getIWM().getPermissionPrefix(world) + "admin.set-world-defaults";
    }

    /**
     * Get all the flags as panel items
     * @return list of all the panel items for this flag type
     */
    @Override
    public @NonNull List<PanelItem> getPanelItems() {
        return getFlags().stream().map(f -> {
            PanelItem i = f.toPanelItem(plugin, user, world, island, false);
            // Replace the click handler with IslandDefaultCycleClick
            i.setClickHandler(new IslandDefaultCycleClick(f.getID()));
            // Replace the description to show the default rank for new islands
            int defaultRank = plugin.getIWM().getWorldSettings(world)
                    .getDefaultIslandFlagNames().getOrDefault(f.getID(), f.getDefaultRank());
            // Build the description showing ranks like the normal protection flag display
            i.setDescription(buildRankDescription(f, defaultRank));
            return i;
        }).toList();
    }

    /**
     * Build the description showing which ranks are allowed/blocked based on the default rank.
     * This mirrors the layout of {@link Flag#createProtectionFlag}.
     */
    private List<String> buildRankDescription(Flag flag, int defaultRank) {
        List<String> desc = new java.util.ArrayList<>();
        desc.add(user.getTranslation("protection.panel.flag-item.description-layout",
                TextVariables.DESCRIPTION, user.getTranslation(flag.getDescriptionReference())));

        RanksManager.getInstance().getRanks().forEach((reference, score) -> {
            String rankName = user.getTranslation(reference);
            if (score > RanksManager.BANNED_RANK && score < defaultRank) {
                desc.add(getRankTranslation("protection.panel.flag-item.blocked-rank", rankName));
            } else if (score <= RanksManager.OWNER_RANK && score > defaultRank) {
                desc.add(getRankTranslation("protection.panel.flag-item.allowed-rank", rankName));
            } else if (score == defaultRank) {
                desc.add(getRankTranslation("protection.panel.flag-item.minimal-rank", rankName));
            }
        });
        return desc;
    }

    /**
     * Gets a rank translation, supporting both MiniMessage format (with [rank] placeholder)
     * and legacy format (without placeholder, rank name concatenated at end).
     */
    private String getRankTranslation(String key, String rankName) {
        String translation = user.getTranslation(key, TextVariables.RANK, rankName);
        if (!translation.contains(rankName)) {
            translation = user.getTranslation(key) + rankName;
        }
        return translation;
    }

}
