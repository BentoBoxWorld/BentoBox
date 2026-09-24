package world.bentobox.bentobox.panels.customizable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.conversations.ConfirmPrompt;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.HideWhen;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.ItemParser;
import world.bentobox.bentobox.util.Util;

/**
 * The island settings panel, built from the {@code panels/settings_panel.yml} template so that
 * admins can lay it out as they wish. It shows the flags of one tab at a time, paged, with the
 * tab buttons, mode switch, reset button and any pinned flags wherever the template puts them.
 * <p>
 * Button types the template can use, via {@code data.type}:
 * <ul>
 * <li>{@code TAB} with {@code data.tab} naming a {@link TabType}: switches to that tab. A tab
 * that does not apply, such as the settings tab when there is no island, is not shown and the
 * button's {@code fallback} is used instead.</li>
 * <li>{@code FLAG}: one slot of the paged flag list. With {@code data.flag} naming a flag ID it
 * shows that flag in that slot instead, and the flag is left out of the paged list.</li>
 * <li>{@code MODE}: cycles the basic/advanced/expert display mode.</li>
 * <li>{@code RESET}: resets every flag to its default; island owner only.</li>
 * <li>{@code NEXT} and {@code PREVIOUS}: paging.</li>
 * </ul>
 * The panel title is the template's {@code title}, translated with {@code [tab]} (the active
 * tab's name) and {@code [world_name]}.
 *
 * @author tastybento
 * @since 3.23.0
 */
public class SettingsPanel extends AbstractPanel implements PanelListener {

    /** Name of the template file, without extension. */
    public static final String SETTINGS_PANEL = "settings_panel";
    /** Button type: tab selector. */
    public static final String TAB = "TAB";
    /** Button type: a flag, paged or pinned. */
    public static final String FLAG = "FLAG";
    /** Button type: display mode switch. */
    public static final String MODE = "MODE";
    /** Button type: reset all flags to their defaults. */
    public static final String RESET = "RESET";

    private static final String PROTECTION_PANEL = "protection.panel.";
    private static final String CLICK_TO_SWITCH = PROTECTION_PANEL + "mode.click-to-switch";
    private static final String WORLD_NAME = "[world_name]";
    private static final String TAB_PARAM = "[tab]";
    private static final String TAB_KEY = "tab";
    private static final String FLAG_KEY = "flag";
    private static final String TYPE_KEY = "type";
    private static final String TITLE = ".title";
    private static final String DESCRIPTION = ".description";

    /**
     * The tabs a settings panel can show.
     */
    public enum TabType {
        /** Protection flags of the island. */
        PROTECTION(Flag.Type.PROTECTION, Material.SHIELD, "PROTECTION"),
        /** Settings of the island. */
        SETTING(Flag.Type.SETTING, Material.COMPARATOR, "SETTING"),
        /**
         * Read-only view of the protection flags that apply off-island, shown to a player who is
         * not on an island.
         */
        WORLD_PROTECTION(Flag.Type.PROTECTION, Material.STONE_BRICKS, "WORLD_DEFAULTS");

        private final Flag.Type flagType;
        private final Material icon;
        private final String localeKey;

        TabType(Flag.Type flagType, Material icon, String localeKey) {
            this.flagType = flagType;
            this.icon = icon;
            this.localeKey = localeKey;
        }

        /**
         * @return the type of flag this tab shows
         */
        public Flag.Type getFlagType() {
            return flagType;
        }

        /**
         * @return the locale key under {@code protection.panel.} for this tab's default title and description
         */
        public String getLocaleKey() {
            return localeKey;
        }

        /**
         * @return whether the tab is read-only: its flag items get no click handler
         */
        public boolean isReadOnly() {
            return this == WORLD_PROTECTION;
        }
    }

    private final World world;
    @Nullable
    private final Island island;
    private final List<TabType> tabs;
    private final Flag.Mode defaultMode;
    private final Map<TabType, Flag.Mode> modes = new EnumMap<>(TabType.class);
    private final Map<TabType, ItemTemplateRecord> tabTemplates = new EnumMap<>(TabType.class);
    /**
     * For each FLAG-typed slot in template order, its ordinal among the slots that are not pinned
     * to a specific flag, i.e. its position in the page.
     */
    private final Map<Integer, Integer> pagedOrdinals = new HashMap<>();
    private int pagedSlots;
    private TabType activeTab;
    /** The flags of the active tab that the viewer may see, in display order. */
    private List<Flag> pagedFlags = new ArrayList<>();
    private List<String> hiddenFlags = new ArrayList<>();
    @Nullable
    private TemplatedPanel panel;
    private boolean refreshing;
    private boolean closed;
    private boolean deferringSaves;

    /**
     * @param command the command that opens the panel, which provides the plugin and game mode
     * @param user the viewer
     * @param world the world the settings are for
     * @param island the island, or null if the viewer is not on one
     * @param tabs the tabs to show, in order; the first is shown initially
     * @param defaultMode the display mode each tab starts in
     */
    protected SettingsPanel(@NonNull CompositeCommand command, @NonNull User user, @NonNull World world,
            @Nullable Island island, @NonNull List<TabType> tabs, Flag.Mode defaultMode) {
        super(command, user);
        this.world = world;
        this.island = island;
        this.tabs = List.copyOf(tabs);
        this.defaultMode = defaultMode;
        this.activeTab = this.tabs.get(0);
    }

    /**
     * Opens the settings panel for a player. With an island the protection and settings tabs are
     * shown; without one, only the read-only view of the world's protection flags.
     * @param command the island settings command
     * @param user the viewer
     * @param island the island, or null if the viewer is not on one
     * @return {@code false} if the template could not be loaded and nothing was opened
     */
    public static boolean openPanel(@NonNull CompositeCommand command, @NonNull User user, @Nullable Island island) {
        List<TabType> tabs = island == null ? List.of(TabType.WORLD_PROTECTION)
                : List.of(TabType.PROTECTION, TabType.SETTING);
        World world = island == null ? command.getWorld() : island.getWorld();
        return new SettingsPanel(command, user, world, island, tabs, Mode.BASIC).open();
    }

    /**
     * Loads the template and opens the panel.
     * @return {@code false} if the template could not be loaded and nothing was opened
     */
    protected boolean open() {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        if (command.getAddon() instanceof GameModeAddon gma && doesCustomPanelExists(gma, SETTINGS_PANEL)) {
            // The game mode has its own settings panel
            panelBuilder.template(SETTINGS_PANEL, new File(gma.getDataFolder(), "panels"));
        } else {
            panelBuilder.template(SETTINGS_PANEL, new File(plugin.getDataFolder(), "panels"));
        }
        PanelTemplateRecord template = panelBuilder.getPanelTemplate();
        if (template == null) {
            return false;
        }
        analyseTemplate(template);
        panelBuilder.user(user).world(world).island(island).listener(this);
        panelBuilder.registerTypeBuilder(TAB, this::createTabButton);
        panelBuilder.registerTypeBuilder(FLAG, this::createFlagButton);
        panelBuilder.registerTypeBuilder(MODE, this::createModeButton);
        panelBuilder.registerTypeBuilder(RESET, this::createResetButton);
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);
        // Avoid a database write on every click while the panel is open
        if (island != null) {
            island.beginDeferSaves();
            deferringSaves = true;
        }
        prepareFlags();
        panelBuilder.parameters(titleParameters());
        panel = panelBuilder.build();
        return true;
    }

    /**
     * Records which template button describes each tab, and numbers the paged flag slots.
     */
    private void analyseTemplate(PanelTemplateRecord template) {
        int flagSlot = 0;
        for (ItemTemplateRecord[] row : template.content()) {
            for (ItemTemplateRecord rec : row) {
                // A slot's fallback chain may hold other tabs, e.g. the world tab behind the island one
                for (ItemTemplateRecord r = rec; r != null; r = r.fallback()) {
                    if (TAB.equals(String.valueOf(r.dataMap().get(TYPE_KEY)))) {
                        TabType tab = parseTab(r);
                        if (tab != null) {
                            tabTemplates.putIfAbsent(tab, r);
                        }
                    }
                }
                if (rec != null && FLAG.equals(String.valueOf(rec.dataMap().get(TYPE_KEY)))) {
                    if (!rec.dataMap().containsKey(FLAG_KEY)) {
                        pagedOrdinals.put(flagSlot, pagedSlots++);
                    }
                    flagSlot++;
                }
            }
        }
    }

    @Nullable
    private static TabType parseTab(ItemTemplateRecord rec) {
        Object tab = rec.dataMap().get(TAB_KEY);
        return tab == null ? null
                : Util.findFirstMatchingEnum(TabType.class, String.valueOf(tab).toUpperCase(Locale.ENGLISH));
    }

    // ---------------------------------------------------------------------
    // Section: Lifecycle
    // ---------------------------------------------------------------------

    /**
     * Next and previous buttons call this; the panel is refreshed by {@link #refreshPanel()} after
     * every click anyway, so there is nothing to do here.
     */
    @Override
    protected void build() {
        // Refreshed by the listener after the click
    }

    @Override
    protected void onPageChanged() {
        // Refreshed by the listener after the click
    }

    @Override
    public void setup() {
        // Nothing to set up
    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Clicks are handled by the buttons
    }

    @Override
    public void refreshPanel() {
        if (closed || panel == null) {
            return;
        }
        prepareFlags();
        // Mark as refreshing so that the inventory close fired by a reopen is not treated as a
        // true close
        refreshing = true;
        try {
            panel.regenerate(titleParameters());
        } finally {
            refreshing = false;
        }
        closed = false;
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        closed = true;
        // Only stop deferring saves when the panel is truly closed, not during a refresh
        if (!refreshing && deferringSaves && island != null) {
            island.endDeferSaves();
            deferringSaves = false;
        }
    }

    @Override
    public boolean hasClickCooldown() {
        return true;
    }

    @Override
    public boolean isActionableSlot(int rawSlot) {
        if (panel == null) {
            return false;
        }
        PanelItem item = panel.getItems().get(rawSlot);
        return item != null && item.getClickHandler().isPresent();
    }

    // ---------------------------------------------------------------------
    // Section: Flags
    // ---------------------------------------------------------------------

    /**
     * Works out which flags the active tab shows in its current mode, skipping past modes in which
     * the viewer would see nothing.
     */
    private void prepareFlags() {
        hiddenFlags = plugin.getIWM().getHiddenFlags(world);
        Flag.Mode mode = currentMode();
        List<Flag> flags = getFlags(mode);
        int i = 0;
        // Jump past empty modes or modes where all flags are invisible to the viewer
        while (flags.stream().noneMatch(this::isVisibleToUser) && i++ < Flag.Mode.values().length) {
            mode = mode.getNext();
            modes.put(activeTab, mode);
            flags = getFlags(mode);
        }
        // Remove any sub-flags that shouldn't be shown
        flags.removeIf(flag -> flag.isSubFlag() && flag.getHideWhen() != HideWhen.NEVER
                && ((!flag.getParentFlag().isSetForWorld(world) && flag.getHideWhen() == HideWhen.SETTING_FALSE)
                        || (flag.getParentFlag().isSetForWorld(world) && flag.getHideWhen() == HideWhen.SETTING_TRUE)));
        // Ops see hidden flags; nobody else does
        flags.removeIf(flag -> !isVisibleToUser(flag));
        pagedFlags = flags;
    }

    /**
     * @return the flags of the active tab's type, for this game mode, in this mode, sorted by
     * translated name
     */
    private List<Flag> getFlags(Flag.Mode mode) {
        Flag.Type type = activeTab.getFlagType();
        List<Flag> flags = new ArrayList<>(plugin.getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(type))
                // We're stripping colors to avoid weird sorting issues
                .sorted(Comparator.comparing(flag -> Util.stripColor(user.getTranslation(flag.getNameReference()))))
                .toList());
        // Remove any that are not for this game mode, are above the current mode, or belong in the top row
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> (!f.getGameModes().isEmpty()
                && !f.getGameModes().contains(gm)) || f.getMode().isGreaterThan(mode)
                || f.getMode().equals(Flag.Mode.TOP_ROW)));
        return flags;
    }

    private Flag.Mode currentMode() {
        return modes.getOrDefault(activeTab, defaultMode);
    }

    private boolean isVisibleToUser(Flag flag) {
        return user.isOp() || !hiddenFlags.contains(flag.getID());
    }

    /**
     * @return whether the active tab has any flag the viewer may see in any mode
     */
    private boolean hasVisibleFlags() {
        if (user.isOp()) {
            return true;
        }
        var addon = plugin.getIWM().getAddon(world);
        return plugin.getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(activeTab.getFlagType()) && !f.getMode().equals(Mode.TOP_ROW))
                .filter(f -> addon.isEmpty() || f.getGameModes().isEmpty() || f.getGameModes().contains(addon.get()))
                .anyMatch(f -> !hiddenFlags.contains(f.getID()));
    }

    @Override
    protected int getPagedItemCount() {
        return pagedFlags.size();
    }

    @Override
    protected String getPagedItemType() {
        return FLAG;
    }

    @Override
    protected int getItemsPerPage(TemplatedPanel.ItemSlot slot) {
        return Math.max(1, pagedSlots);
    }

    // ---------------------------------------------------------------------
    // Section: Buttons
    // ---------------------------------------------------------------------

    /**
     * A flag slot: the pinned flag if the template names one, else the next flag of the page.
     */
    @Nullable
    private PanelItem createFlagButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        Object pinned = template.dataMap().get(FLAG_KEY);
        if (pinned != null) {
            return createPinnedFlag(String.valueOf(pinned), template);
        }
        Integer ordinal = pagedOrdinals.get(slot.slot());
        if (ordinal == null) {
            return null;
        }
        int index = pageIndex * Math.max(1, pagedSlots) + ordinal;
        if (index >= pagedFlags.size()) {
            return null;
        }
        return createFlagItem(pagedFlags.get(index), template, hiddenFlags.contains(pagedFlags.get(index).getID()));
    }

    /**
     * A flag the template pins to a slot, such as the lock. Island flags need an island; the flag
     * is shown regardless of the hidden-flag list because the admin placed it deliberately.
     */
    @Nullable
    private PanelItem createPinnedFlag(String id, ItemTemplateRecord template) {
        Flag flag = plugin.getFlagsManager().getFlag(id).orElse(null);
        if (flag == null) {
            plugin.logWarning("Settings panel template pins unknown flag " + id);
            return null;
        }
        if (island == null && flag.getType() != Flag.Type.WORLD_SETTING) {
            return null;
        }
        return createFlagItem(flag, template, false);
    }

    @Nullable
    private PanelItem createFlagItem(Flag flag, ItemTemplateRecord template, boolean invisible) {
        PanelItem item = flag.toPanelItem(plugin, user, world, island, invisible, template);
        if (item != null && activeTab.isReadOnly()) {
            // Only an admin may change these values, and the flag's own click handler would just
            // tell the player they are not on an island
            item.setClickHandler(null);
        }
        return item;
    }

    /**
     * A tab button. Not shown for tabs that do not apply in this context.
     */
    @Nullable
    private PanelItem createTabButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        TabType tab = parseTab(template);
        if (tab == null || !tabs.contains(tab)) {
            return null;
        }
        PanelItemBuilder builder = new PanelItemBuilder()
                .icon(template.icon() != null ? template.icon().clone() : new ItemStack(tab.icon))
                .name(tabName(tab, template))
                .description(user.getTranslation(template.description() != null ? template.description()
                        : PROTECTION_PANEL + tab.getLocaleKey() + DESCRIPTION))
                .glow(tab == activeTab)
                .clickHandler((p, u, clickType, s) -> {
                    if (clickAllowed(template, clickType) && tab != activeTab) {
                        activeTab = tab;
                        pageIndex = 0;
                        u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
                    }
                    return true;
                });
        addTooltips(builder, template);
        return builder.build();
    }

    /**
     * @return the translated name of a tab: the template button's title if it has one, else the
     * tab's default locale key
     */
    private String tabName(TabType tab, @Nullable ItemTemplateRecord template) {
        String key = template != null && template.title() != null ? template.title()
                : PROTECTION_PANEL + tab.getLocaleKey() + TITLE;
        return user.getTranslation(key, WORLD_NAME, plugin.getIWM().getFriendlyName(world));
    }

    /**
     * @return the parameters for the panel title
     */
    private String[] titleParameters() {
        return new String[] { TAB_PARAM, tabName(activeTab, tabTemplates.get(activeTab)), WORLD_NAME,
                plugin.getIWM().getFriendlyName(world) };
    }

    /**
     * The display mode button. Its icon may be set per mode in the template's data as
     * {@code basic-icon}, {@code advanced-icon} and {@code expert-icon}; its text comes from the
     * {@code protection.panel.mode} locale keys.
     */
    @Nullable
    private PanelItem createModeButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        if (!hasVisibleFlags()) {
            return null;
        }
        Flag.Mode mode = currentMode();
        String key = mode.name().toLowerCase(Locale.ENGLISH);
        String nextKey = mode.getNext().name().toLowerCase(Locale.ENGLISH);
        Material defaultIcon = switch (mode) {
        case ADVANCED -> Material.GOLD_INGOT;
        case EXPERT -> Material.NETHER_BRICK;
        default -> Material.IRON_INGOT;
        };
        Object modeIcon = template.dataMap().get(key + "-icon");
        ItemStack icon = modeIcon != null ? ItemParser.parse(String.valueOf(modeIcon), new ItemStack(defaultIcon))
                : template.icon() != null ? template.icon().clone() : new ItemStack(defaultIcon);
        PanelItemBuilder builder = new PanelItemBuilder().icon(icon)
                .name(user.getTranslation(PROTECTION_PANEL + "mode." + key + ".name"))
                .description(user.getTranslation(PROTECTION_PANEL + "mode." + key + DESCRIPTION), "",
                        user.getTranslation(CLICK_TO_SWITCH, TextVariables.NEXT,
                                user.getTranslation(PROTECTION_PANEL + "mode." + nextKey + ".name")))
                .clickHandler((p, u, clickType, s) -> {
                    if (clickAllowed(template, clickType)) {
                        modes.put(activeTab, currentMode().getNext());
                        pageIndex = 0;
                        u.getPlayer().playSound(u.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
                    }
                    return true;
                });
        addTooltips(builder, template);
        return builder.build();
    }

    /**
     * The reset-to-default button, for the island owner only.
     */
    @Nullable
    private PanelItem createResetButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        if (island == null || !user.getUniqueId().equals(island.getOwner())) {
            return null;
        }
        PanelItemBuilder builder = new PanelItemBuilder()
                .icon(template.icon() != null ? template.icon().clone() : new ItemStack(Material.TNT))
                .name(user.getTranslation(template.title() != null ? template.title()
                        : PROTECTION_PANEL + "reset-to-default.name"))
                .description(user.getTranslation(template.description() != null ? template.description()
                        : PROTECTION_PANEL + "reset-to-default.description"))
                .clickHandler((p, u, clickType, s) -> {
                    if (clickAllowed(template, clickType)) {
                        confirmReset(u);
                    }
                    return true;
                });
        addTooltips(builder, template);
        return builder.build();
    }

    private void confirmReset(User u) {
        u.closeInventory(); // let them see the confirmation
        String confirmation = u.getTranslationOrNothing(PROTECTION_PANEL + "reset-to-default.confirm");
        if (confirmation.isBlank()) {
            confirmation = "confirm";
        }
        new ConversationFactory(BentoBox.getInstance()).withModality(true).withLocalEcho(false)
                .withTimeout(90)
                .withFirstPrompt(new ConfirmPrompt(u, plugin, PROTECTION_PANEL + "reset-to-default.instructions",
                        confirmation, () -> {
                            Objects.requireNonNull(island).setFlagsDefaults();
                            u.getPlayer().playSound(u.getLocation(), Sound.ENTITY_TNT_PRIMED, 1F, 1F);
                        }))
                .buildConversation(u.getPlayer()).begin();
    }

    /**
     * @return whether the template lets this click type act: any click if the button declares no
     * actions, else only the declared click types ({@code UNKNOWN} matching any)
     */
    private static boolean clickAllowed(ItemTemplateRecord template, ClickType clickType) {
        return template.actions().isEmpty() || template.actions().stream()
                .anyMatch(a -> a.clickType() == clickType || a.clickType() == ClickType.UNKNOWN);
    }

    // ---------------------------------------------------------------------
    // Section: Getters
    // ---------------------------------------------------------------------

    /**
     * @return the tab currently shown
     */
    public TabType getActiveTab() {
        return activeTab;
    }

    /**
     * @return the display mode of the active tab
     */
    public Flag.Mode getMode() {
        return currentMode();
    }

    /**
     * @return the island the panel is about, or null
     */
    @Nullable
    public Island getIsland() {
        return island;
    }

    /**
     * @return the world the panel is about
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the current page, starting at 0
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return the panel that is open, or null if none
     */
    @Nullable
    public TemplatedPanel getPanel() {
        return panel;
    }
}
