package world.bentobox.bentobox.panels.settings;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Implements a {@link Tab} that shows settings for
 * {@link world.bentobox.bentobox.api.flags.Flag.Type#PROTECTION}, {@link world.bentobox.bentobox.api.flags.Flag.Type#SETTING}, {@link world.bentobox.bentobox.api.flags.Flag.Type#WORLD_SETTING}
 * @author tastybento
 * @since 1.6.0
 *
 */
public class SettingsTab implements Tab, ClickHandler {

    protected static final String PROTECTION_PANEL = "protection.panel.";
    private static final String CLICK_TO_SWITCH = PROTECTION_PANEL + "mode.click-to-switch";
    protected BentoBox plugin = BentoBox.getInstance();
    protected Flag.Type type;
    protected User user;
    protected World world;
    protected Island island;

    /**
     * Show a tab of settings
     * @param world - world
     * @param user - user who is viewing the tab
     * @param island - the island
     * @param type - flag type
     */
    public SettingsTab(World world, User user, Island island, Type type) {
        this.world = world;
        this.user = user;
        this.island = island;
        this.type = type;
    }

    /**
     * Show a tab of settings
     * @param world - world
     * @param user - user who is viewing the tab
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
                // We're stripping colors to avoid weird sorting issues
                .sorted(Comparator.comparing(flag -> ChatColor.stripColor(user.getTranslation(flag.getNameReference()))))
                .collect(Collectors.toList());
        // Remove any that are not for this game mode
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> !f.getGameModes().isEmpty() && !f.getGameModes().contains(gm)));
        // Remove any that are the wrong rank or that will be on the top row
        Flag.Mode mode = plugin.getPlayers().getFlagsDisplayMode(user.getUniqueId());
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> f.getMode().isGreaterThan(mode) ||
                f.getMode().equals(Flag.Mode.TOP_ROW)));
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
    @NonNull
    public List<@Nullable PanelItem> getPanelItems() {
        List<Flag> flags = getFlags();
        int i = 0;
        // Jump past empty tabs
        while (flags.isEmpty() && i++ < Flag.Mode.values().length) {
            plugin.getPlayers().setFlagsDisplayMode(user.getUniqueId(), plugin.getPlayers().getFlagsDisplayMode(user.getUniqueId()).getNext());
            flags = getFlags();
        }
        return flags.stream().map((f -> f.toPanelItem(plugin, user, island, plugin.getIWM().getHiddenFlags(world).contains(f.getID())))).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, PanelItem> getTabIcons() {
        Map<Integer, PanelItem> icons = new HashMap<>();
        // Add the lock icon - we want it to be displayed no matter the tab
        if (island != null) {
            icons.put(5, Flags.LOCK.toPanelItem(plugin, user, island, false));
        }
        // Add the mode icon
        switch(plugin.getPlayers().getFlagsDisplayMode(user.getUniqueId())) {
        case ADVANCED:
            icons.put(7, new PanelItemBuilder().icon(Material.GOLD_INGOT)
                    .name(user.getTranslation(PROTECTION_PANEL + "mode.advanced.name"))
                    .description(user.getTranslation(PROTECTION_PANEL + "mode.advanced.description"), "",
                            user.getTranslation(CLICK_TO_SWITCH,
                                    TextVariables.NEXT, user.getTranslation(PROTECTION_PANEL + "mode.expert.name")))
                    .clickHandler(this)
                    .build());
            break;
        case EXPERT:
            icons.put(7, new PanelItemBuilder().icon(Material.NETHER_BRICK)
                    .name(user.getTranslation(PROTECTION_PANEL + "mode.expert.name"))
                    .description(user.getTranslation(PROTECTION_PANEL + "mode.expert.description"), "",
                            user.getTranslation(CLICK_TO_SWITCH,
                                    TextVariables.NEXT, user.getTranslation(PROTECTION_PANEL + "mode.basic.name")))
                    .clickHandler(this)
                    .build());
            break;
        default:
            icons.put(7, new PanelItemBuilder().icon(Material.IRON_INGOT)
                    .name(user.getTranslation(PROTECTION_PANEL + "mode.basic.name"))
                    .description(user.getTranslation(PROTECTION_PANEL + "mode.basic.description"), "",
                            user.getTranslation(CLICK_TO_SWITCH,
                                    TextVariables.NEXT, user.getTranslation(PROTECTION_PANEL + "mode.advanced.name")))
                    .clickHandler(this)
                    .build());
        }
        // Add the reset everything to default - it's only in the player's settings panel
        if (island != null && user.getUniqueId().equals(island.getOwner())) {
            icons.put(8, new PanelItemBuilder().icon(Material.TNT)
                    .name(user.getTranslation(PROTECTION_PANEL + "reset-to-default.name"))
                    .description(user.getTranslation(PROTECTION_PANEL + "reset-to-default.description"))
                    .clickHandler((panel, user1, clickType, slot) -> {
                        island.setFlagsDefaults();
                        user.getPlayer().playSound(user.getLocation(), Sound.ENTITY_TNT_PRIMED, 1F, 1F);
                        return true;
                    })
                    .build());
        }
        return icons;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.Tab#getPermission()
     */
    @Override
    public String getPermission() {
        // All of these tabs can be seen by anyone
        return "";
    }

    /**
     * @return the type
     */
    public Flag.Type getType() {
        return type;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Cycle the mode
        plugin.getPlayers().setFlagsDisplayMode(user.getUniqueId(), plugin.getPlayers().getFlagsDisplayMode(user.getUniqueId()).getNext());
        if (panel instanceof TabbedPanel) {
            TabbedPanel tp = ((TabbedPanel)panel);
            tp.setActivePage(0);
            tp.refreshPanel();
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
        }
        return true;
    }

}
