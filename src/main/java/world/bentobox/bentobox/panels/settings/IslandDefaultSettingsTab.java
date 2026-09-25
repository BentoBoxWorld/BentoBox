package world.bentobox.bentobox.panels.settings;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.IslandDefaultCycleClick;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Implements a {@link Tab} that enables the default island protection settings to be changed.
 * These are the protection flag rank values that new islands will receive.
 * @author tastybento
 * @since 3.15.0
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
     * Get all the flags as panel items, each showing the default rank new islands will get
     * @return list of all the panel items for this flag type
     */
    @Override
    public @NonNull List<PanelItem> getPanelItems() {
        return getFlags().stream().map(f -> {
            int defaultRank = plugin.getIWM().getWorldSettings(world)
                    .getDefaultIslandFlagNames().getOrDefault(f.getID(), f.getDefaultRank());
            PanelItem i = f.toPanelItemForRank(user, defaultRank, null);
            // Clicks change the default rank rather than an island's
            i.setClickHandler(new IslandDefaultCycleClick(f.getID()));
            return i;
        }).toList();
    }

}
