package world.bentobox.bentobox.panels.customizable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.panels.customizable.SettingsPanel.TabType;
import world.bentobox.bentobox.util.ItemParser;

/**
 * Tests for {@link SettingsPanel}, using the shipped {@code panels/settings_panel.yml} template so
 * that the default layout is what is tested.
 */
class SettingsPanelTest extends RanksManagerTestSetup {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    /** Slots of the default template */
    private static final int PROTECTION_TAB = 1;
    private static final int SETTING_TAB = 2;
    private static final int CHANGE_SETTINGS = 4;
    private static final int LOCK = 5;
    private static final int MODE = 7;
    private static final int RESET = 8;
    private static final int FIRST_FLAG = 9;
    private static final int PREVIOUS = 46;
    private static final int NEXT = 52;

    @Mock
    private User user;
    @Mock
    private CompositeCommand command;
    @Mock
    private GameModeAddon gma;

    private UUID uuid;
    private final List<Flag> flags = new ArrayList<>();
    private final List<String> hidden = new ArrayList<>();
    private final Map<String, String> texts = new HashMap<>();

    private MockedStatic<ItemParser> mockedItemParser;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Item parsing needs a real item factory, which the mocked Bukkit does not provide, so
        // parse plain material names directly
        mockedItemParser = Mockito.mockStatic(ItemParser.class);
        mockedItemParser.when(() -> ItemParser.parse(anyString()))
                .thenAnswer(inv -> new ItemStack(Material.valueOf(inv.getArgument(0, String.class))));
        mockedItemParser.when(() -> ItemParser.parse(any(), any())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            return text == null || text.isBlank() ? inv.getArgument(1) : new ItemStack(Material.valueOf(text));
        });
        // Plugin
        when(plugin.getDataFolder()).thenReturn(RESOURCES.toFile());
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(fm.getFlags()).thenReturn(flags);
        when(fm.getFlag(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            if (Flags.LOCK.getID().equals(id)) {
                return Optional.of(Flags.LOCK);
            }
            if (Flags.CHANGE_SETTINGS.getID().equals(id)) {
                return Optional.of(Flags.CHANGE_SETTINGS);
            }
            return flags.stream().filter(f -> f.getID().equals(id)).findFirst();
        });
        when(iwm.getHiddenFlags(any())).thenReturn(hidden);

        // Command
        when(command.getPlugin()).thenReturn(plugin);
        when(command.getWorld()).thenReturn(world);

        // User
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getLocation()).thenReturn(location);
        when(user.getWorld()).thenReturn(world);
        when(user.isOp()).thenReturn(false);
        Answer<String> answer = inv -> {
            Object[] args = inv.getArguments();
            String text = texts.getOrDefault((String) args[0], (String) args[0]);
            for (int i = 1; i + 1 < args.length; i += 2) {
                text = text.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
            }
            return text;
        };
        when(user.getTranslation(anyString())).thenAnswer(answer);
        when(user.getTranslation(anyString(), any(String[].class))).thenAnswer(answer);
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), any(String[].class))).thenReturn("");

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getFlag(any())).thenReturn(MEMBER_RANK);
        when(island.isAllowed(any(Flag.class))).thenReturn(true);

        // Some flags of each type, all in the basic mode
        flags.add(flag("P_ALPHA", Type.PROTECTION, Mode.BASIC));
        flags.add(flag("P_BETA", Type.PROTECTION, Mode.BASIC));
        flags.add(flag("S_GAMMA", Type.SETTING, Mode.BASIC));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        TemplateReader.clearPanels();
        mockedItemParser.close();
        super.tearDown();
    }

    private static Flag flag(String id, Type type, Mode mode) {
        return new Flag.Builder(id, Material.STONE).type(type).mode(mode).build();
    }

    private static SettingsPanel open(CompositeCommand command, User user, boolean withIsland, Island island) {
        assertTrue(SettingsPanel.openPanel(command, user, withIsland ? island : null));
        return (SettingsPanel) PanelListenerManager.getOpenPanels().get(user.getUniqueId()).getListener()
                .orElseThrow();
    }

    private SettingsPanel openWithIsland() {
        return open(command, user, true, island);
    }

    private Map<Integer, PanelItem> items(SettingsPanel sp) {
        TemplatedPanel panel = sp.getPanel();
        assertNotNull(panel);
        return panel.getItems();
    }

    private static Material material(Map<Integer, PanelItem> items, int slot) {
        return items.get(slot).getItem().getType();
    }

    private static void click(Map<Integer, PanelItem> items, int slot, User user, ClickType click) {
        items.get(slot).getClickHandler().orElseThrow().onClick(null, user, click, slot);
    }

    /**
     * The default template reproduces the old panel: tabs at 1 and 2, change-settings and lock at
     * 4 and 5, the mode switch at 7, reset at 8, flags from 9, and filler elsewhere.
     */
    @Test
    void testDefaultLayoutWithIsland() {
        SettingsPanel sp = openWithIsland();
        Map<Integer, PanelItem> items = items(sp);

        assertEquals(Material.SHIELD, material(items, PROTECTION_TAB));
        assertTrue(items.get(PROTECTION_TAB).isGlow(), "active tab glows");
        assertEquals("protection.panel.PROTECTION.title", items.get(PROTECTION_TAB).getName());
        assertEquals(Material.COMPARATOR, material(items, SETTING_TAB));
        assertFalse(items.get(SETTING_TAB).isGlow());
        assertEquals(Material.CRAFTING_TABLE, material(items, CHANGE_SETTINGS));
        assertEquals(Material.TRIPWIRE_HOOK, material(items, LOCK));
        assertEquals(Material.IRON_INGOT, material(items, MODE));
        assertEquals(Material.TNT, material(items, RESET));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, 0));
        // The two protection flags, sorted by name
        assertEquals("protection.panel.flag-item.name-layout", items.get(FIRST_FLAG).getName());
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertEquals(Material.STONE, material(items, FIRST_FLAG + 1));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 2));
        // No paging needed
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, PREVIOUS));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, NEXT));
        assertEquals(TabType.PROTECTION, sp.getActiveTab());
        assertEquals(Mode.BASIC, sp.getMode());
        // Title is translated with the tab name and world name
        verify(user).getTranslation("panels.settings.title", "[tab]", "protection.panel.PROTECTION.title",
                "[world_name]", "BSkyBlock");
    }

    /**
     * Without an island only the read-only world protection tab is shown. It sits in the
     * protection tab's slot because it is that button's fallback.
     */
    @Test
    void testNoIslandShowsWorldProtectionOnly() {
        SettingsPanel sp = open(command, user, false, null);
        Map<Integer, PanelItem> items = items(sp);

        assertEquals(TabType.WORLD_PROTECTION, sp.getActiveTab());
        assertEquals(Material.STONE_BRICKS, material(items, PROTECTION_TAB));
        assertEquals("protection.panel.WORLD_DEFAULTS.title", items.get(PROTECTION_TAB).getName());
        // No settings tab, no pinned island flags, no reset
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, SETTING_TAB));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, CHANGE_SETTINGS));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, LOCK));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, RESET));
        // Protection flags are shown but cannot be clicked
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertTrue(items.get(FIRST_FLAG).getClickHandler().isEmpty());
        assertTrue(items.get(FIRST_FLAG + 1).getClickHandler().isEmpty());
        verify(island, never()).beginDeferSaves();
    }

    @Test
    void testTemplateMissingReturnsFalse(@TempDir Path empty) {
        when(plugin.getDataFolder()).thenReturn(empty.toFile());
        assertFalse(SettingsPanel.openPanel(command, user, island));
        assertNull(PanelListenerManager.getOpenPanels().get(uuid));
    }

    /**
     * A game mode's own template takes precedence over the plugin's.
     */
    @Test
    void testGameModeTemplateOverrides(@TempDir Path addonFolder) throws IOException {
        Path panels = Files.createDirectories(addonFolder.resolve("panels"));
        String yml = Files.readString(RESOURCES.resolve("panels").resolve("settings_panel.yml"), StandardCharsets.UTF_8)
                .replace("title: panels.settings.title", "title: custom.title");
        Files.writeString(panels.resolve("settings_panel.yml"), yml, StandardCharsets.UTF_8);
        when(gma.getDataFolder()).thenReturn(addonFolder.toFile());
        when(command.getAddon()).thenReturn(gma);

        SettingsPanel sp = openWithIsland();

        assertEquals("custom.title", sp.getPanel().getName());
    }

    @Test
    void testTabSwitch() {
        SettingsPanel sp = openWithIsland();
        click(items(sp), SETTING_TAB, user, ClickType.LEFT);
        assertEquals(TabType.SETTING, sp.getActiveTab());
        sp.refreshPanel();
        Map<Integer, PanelItem> items = items(sp);

        assertTrue(items.get(SETTING_TAB).isGlow());
        assertFalse(items.get(PROTECTION_TAB).isGlow());
        // One setting flag
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 1));
        verify(user).getTranslation("panels.settings.title", "[tab]", "protection.panel.SETTING.title",
                "[world_name]", "BSkyBlock");
    }

    /**
     * Only a left click switches tabs, as the template's action says.
     */
    @Test
    void testTabSwitchIgnoresOtherClicks() {
        SettingsPanel sp = openWithIsland();
        click(items(sp), SETTING_TAB, user, ClickType.RIGHT);
        assertEquals(TabType.PROTECTION, sp.getActiveTab());
    }

    /**
     * Flags above the current mode are hidden until the mode is cycled up to them.
     */
    @Test
    void testModeCycling() {
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        flags.add(flag("P_EXPERT", Type.PROTECTION, Mode.EXPERT));
        SettingsPanel sp = openWithIsland();
        Map<Integer, PanelItem> items = items(sp);
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 2));

        click(items, MODE, user, ClickType.LEFT);
        assertEquals(Mode.ADVANCED, sp.getMode());
        sp.refreshPanel();
        items = items(sp);
        assertEquals(Material.GOLD_INGOT, material(items, MODE));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 2));

        click(items, MODE, user, ClickType.LEFT);
        assertEquals(Mode.EXPERT, sp.getMode());
        sp.refreshPanel();
        items = items(sp);
        assertEquals(Material.NETHER_BRICK, material(items, MODE));
        assertEquals(Material.STONE, material(items, FIRST_FLAG + 2));
    }

    /**
     * The mode is remembered per tab.
     */
    @Test
    void testModePerTab() {
        SettingsPanel sp = openWithIsland();
        click(items(sp), MODE, user, ClickType.LEFT);
        assertEquals(Mode.ADVANCED, sp.getMode());
        click(items(sp), SETTING_TAB, user, ClickType.LEFT);
        assertEquals(Mode.BASIC, sp.getMode());
    }

    /**
     * 40 flags need two pages of 36: the next button appears, and after paging the previous one.
     */
    @Test
    void testPaging() {
        flags.clear();
        IntStream.range(0, 40).forEach(i -> flags.add(flag(String.format("P_%02d", i), Type.PROTECTION, Mode.BASIC)));
        SettingsPanel sp = openWithIsland();
        Map<Integer, PanelItem> items = items(sp);
        assertEquals(Material.ARROW, material(items, NEXT));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, PREVIOUS));
        assertEquals(Material.STONE, material(items, FIRST_FLAG + 35));

        click(items, NEXT, user, ClickType.LEFT);
        assertEquals(1, sp.getPageIndex());
        sp.refreshPanel();
        items = items(sp);
        assertEquals(Material.ARROW, material(items, PREVIOUS));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, NEXT));
        // Four flags on the second page
        assertEquals(Material.STONE, material(items, FIRST_FLAG + 3));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 4));

        click(items, PREVIOUS, user, ClickType.LEFT);
        assertEquals(0, sp.getPageIndex());
    }

    /**
     * Hidden flags are not shown to players but are shown to ops.
     */
    @Test
    void testHiddenFlags() {
        hidden.add("P_ALPHA");
        SettingsPanel sp = openWithIsland();
        Map<Integer, PanelItem> items = items(sp);
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 1));

        when(user.isOp()).thenReturn(true);
        sp.refreshPanel();
        items = items(sp);
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertEquals(Material.STONE, material(items, FIRST_FLAG + 1));
    }

    /**
     * When every flag of the tab is hidden from the player, the mode button is not shown either.
     */
    @Test
    void testNoModeButtonWhenNothingVisible() {
        hidden.add("P_ALPHA");
        hidden.add("P_BETA");
        SettingsPanel sp = openWithIsland();
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items(sp), MODE));
    }

    @Test
    void testResetOnlyForOwner() {
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        SettingsPanel sp = openWithIsland();
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items(sp), RESET));
    }

    /**
     * Island saves are deferred while the panel is open and resumed when it is truly closed.
     */
    @Test
    void testDeferredSaves() {
        SettingsPanel sp = openWithIsland();
        verify(island).beginDeferSaves();
        verify(island, never()).endDeferSaves();

        sp.refreshPanel();
        verify(island, never()).endDeferSaves();

        sp.onInventoryClose(mock(InventoryCloseEvent.class));
        verify(island).endDeferSaves();
        // A closed panel is not refreshed
        sp.refreshPanel();
        verify(island).beginDeferSaves();
    }

    @Test
    void testClickCooldown() {
        SettingsPanel sp = openWithIsland();
        assertTrue(sp.hasClickCooldown());
        assertTrue(sp.isActionableSlot(PROTECTION_TAB));
        assertTrue(sp.isActionableSlot(FIRST_FLAG));
        assertFalse(sp.isActionableSlot(0), "filler does nothing");
        assertFalse(sp.isActionableSlot(53));
    }

    /**
     * The panel carries the island so that flag click handlers can find it.
     */
    @Test
    void testPanelIslandContext() {
        SettingsPanel sp = openWithIsland();
        assertEquals(island, sp.getPanel().getIsland());
        assertEquals(island, sp.getIsland());
        assertEquals(world, sp.getWorld());
        assertEquals(Optional.of(world), sp.getPanel().getWorld());
    }

    /**
     * A sub-flag is hidden when its parent's world setting says so.
     */
    @Test
    void testSubFlagHiding() {
        Flag sub = new Flag.Builder("P_SUB", Material.STONE).type(Type.PROTECTION).mode(Mode.BASIC)
                .hideWhen(Flag.HideWhen.SETTING_FALSE).build();
        Flag parent = new Flag.Builder("P_PARENT", Material.STONE).type(Type.PROTECTION).mode(Mode.BASIC)
                .subflags(sub).build();
        flags.clear();
        flags.add(parent);
        flags.add(sub);
        // Parent is off for the world, so the sub-flag is hidden
        SettingsPanel sp = openWithIsland();
        Map<Integer, PanelItem> items = items(sp);
        assertEquals(Material.STONE, material(items, FIRST_FLAG));
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items, FIRST_FLAG + 1));
    }

    @Test
    void testTabTypeAccessors() {
        assertEquals(Type.PROTECTION, TabType.PROTECTION.getFlagType());
        assertEquals(Type.SETTING, TabType.SETTING.getFlagType());
        assertEquals(Type.PROTECTION, TabType.WORLD_PROTECTION.getFlagType());
        assertEquals("WORLD_DEFAULTS", TabType.WORLD_PROTECTION.getLocaleKey());
        assertTrue(TabType.WORLD_PROTECTION.isReadOnly());
        assertFalse(TabType.PROTECTION.isReadOnly());
    }

    @Test
    void testUnknownPinnedFlagIsSkipped(@TempDir Path addonFolder) throws IOException {
        Path panels = Files.createDirectories(addonFolder.resolve("panels"));
        String yml = Files.readString(RESOURCES.resolve("panels").resolve("settings_panel.yml"), StandardCharsets.UTF_8)
                .replace("flag: LOCK", "flag: NO_SUCH_FLAG");
        Files.writeString(panels.resolve("settings_panel.yml"), yml, StandardCharsets.UTF_8);
        when(gma.getDataFolder()).thenReturn(addonFolder.toFile());
        when(command.getAddon()).thenReturn(gma);

        SettingsPanel sp = openWithIsland();

        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, material(items(sp), LOCK));
        verify(plugin).logWarning(eq("Settings panel template pins unknown flag NO_SUCH_FLAG"));
    }
}
