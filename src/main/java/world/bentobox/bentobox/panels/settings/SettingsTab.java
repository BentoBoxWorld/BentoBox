package world.bentobox.bentobox.panels.settings;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Implements a {@link Tab} that shows settings for
 * {@link Flag.Type#PROTECTION}, {@link Flag.Type#SETTING}, {@link Flag.Type#WORLD_SETTING}
 * @author tastybento
 * @since 1.6.0
 *
 */
public class SettingsTab implements Tab {

    protected static final String PROTECTION_PANEL = "protection.panel.";
    protected BentoBox plugin = BentoBox.getInstance();
    protected Flag.Type type;
    protected User user;
    protected World world;

    /**
     * @param world - world
     * @param user - user
     * @param type - flag type
     */
    public SettingsTab(World world, User user, Type type) {
        this.world = world;
        this.user = user;
        this.type = type;
    }

    /**
     * @return list of flags that will be shown in this panel
     */
    protected List<Flag> getFlags() {
        // Get a list of flags of the correct type and sort by the translated names
        List<Flag> flags = plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(type))
                .sorted(Comparator.comparing(flag -> user.getTranslation(flag.getNameReference())))
                .collect(Collectors.toList());
        // Remove any that are not for this game mode
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> !f.getGameModes().isEmpty() && !f.getGameModes().contains(gm)));
        return flags;
    }

    /**
     * Get the icon for this tab
     * @return panel item
     */
    @Override
    public PanelItem getIcon() {
        PanelItemBuilder pib = new PanelItemBuilder();
        // Set the icon
        pib.icon(type.getIcon());
        pib.name(getName());
        pib.description(user.getTranslation(PROTECTION_PANEL + type.toString() + ".description"));
        return pib.build();
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.Tab#getName()
     */
    @Override
    public String getName() {
        return user.getTranslation(PROTECTION_PANEL + type.toString() + ".title", "[world_name]", plugin.getIWM().getFriendlyName(world));
    }

    /**
     * Get all the flags as panel items
     * @return list of all the panel items for this flag type
     */
    @Override
    public List<PanelItem> getPanelItems() {
        return getFlags().stream().map((f -> f.toPanelItem(plugin, user, plugin.getIWM().getHiddenFlags(world).contains(f.getID())))).collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.Tab#getPermission()
     */
    @Override
    public String getPermission() {
        // All of these tabs can be seen by anyone
        return "";
    }

}
