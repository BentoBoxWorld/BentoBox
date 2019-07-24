package world.bentobox.bentobox.panels.settings;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.World;

import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.WorldToggleClick;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Implements a tab that shows all three types of settings
 * @author tastybento
 *
 */
public class WorldDefaultSettingsTab extends SettingsTab implements Tab {

    /**
     * @param world - world
     * @param user - user
     * @param type - flag type
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
        // Set the icon TODO could be different
        pib.icon(type.getIcon());
        pib.name(getName());
        // Different description
        pib.description(user.getTranslation(PROTECTION_PANEL + "world-defaults.description"));
        return pib.build();
    }

    @Override
    public String getName() {
        // Different name
        return user.getTranslation(PROTECTION_PANEL + "world-defaults.title", "[world_name]", plugin.getIWM().getFriendlyName(world));
    }

    /**
     * Get all the flags as panel items
     * @return list of all the panel items for this flag type
     */
    @Override
    public List<PanelItem> getPanelItems() {
        // Different descriptipn and click handlers
        return getFlags().stream().map(f -> {
            PanelItem i = f.toPanelItem(plugin, user, plugin.getIWM().getHiddenFlags(world).contains(f.getID()));
            // Replace the click handler with a new one
            i.setClickHandler(new WorldToggleClick(f.getID()));
            // Replace the description
            String worldSetting = f.isSetForWorld(user.getWorld()) ? user.getTranslation("protection.panel.flag-item.setting-active")
                    : user.getTranslation("protection.panel.flag-item.setting-disabled");
            i.setDescription(Collections.singletonList(
                    user.getTranslation("protection.panel.flag-item.setting-layout", TextVariables.DESCRIPTION, user.getTranslation(f.getDescriptionReference())
                            , "[setting]", worldSetting)));
            return i;
        }).collect(Collectors.toList());
    }


}
