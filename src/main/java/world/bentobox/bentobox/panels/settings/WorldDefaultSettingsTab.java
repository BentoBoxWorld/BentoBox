package world.bentobox.bentobox.panels.settings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.WorldToggleClick;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Implements a {@link Tab} that enables the default world protection settings to be set
 * @author tastybento
 * @since 1.6.0
 *
 */
public class WorldDefaultSettingsTab extends SettingsTab implements Tab {

    /**
     * @param world - world
     * @param user - user
     */
    public WorldDefaultSettingsTab(World world, User user) {
        super(world, user, Type.PROTECTION);
    }

    /**
     * Get the icon for this tab
     * @return panel item
     */
    @Override
    public PanelItem getIcon() {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.icon(Material.STONE_BRICKS);
        pib.name(getName());
        // Different description
        pib.description(user.getTranslation(PROTECTION_PANEL + "WORLD_DEFAULTS.description"));
        return pib.build();
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.panels.settings.SettingsTab#getName()
     */
    @Override
    public String getName() {
        // Different name
        return user.getTranslation(PROTECTION_PANEL + "WORLD_DEFAULTS.title", "[world_name]", plugin.getIWM().getFriendlyName(world));
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.panels.settings.SettingsTab#getPermission()
     */
    @Override
    public String getPermission() {
        // This permission should be in the Game Mode Addon's permissions list
        return plugin.getIWM().getPermissionPrefix(world) + "admin.set-world-defaults";
    }

    /**
     * Get all the flags as panel items
     * @return list of all the panel items for this flag type
     */
    @Override
    public List<PanelItem> getPanelItems() {
        // Different description and click handlers
        return getFlags().stream().map(f -> {
            PanelItem i = f.toPanelItem(plugin, user, null, false);
            // Replace the click handler with WorldToggleClick
            i.setClickHandler(new WorldToggleClick(f.getID()));
            // Replace the description
            String worldSetting = f.isSetForWorld(user.getWorld()) ? user.getTranslation("protection.panel.flag-item.setting-active")
                    : user.getTranslation("protection.panel.flag-item.setting-disabled");
            i.setDescription(Arrays.asList(user.getTranslation("protection.panel.flag-item.setting-layout",
                    TextVariables.DESCRIPTION, user.getTranslation(f.getDescriptionReference()),
                    "[setting]", worldSetting).split("\n")));
            return i;
        }).collect(Collectors.toList());
    }

}
