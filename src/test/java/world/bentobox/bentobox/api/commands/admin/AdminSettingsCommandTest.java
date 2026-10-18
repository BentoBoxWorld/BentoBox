package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.customizable.SettingsPanel;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class AdminSettingsCommandTest extends RanksManagerTestSetup {

    private AdminSettingsCommand asc;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Location spawnPoint;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
 
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        // Island - player has island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);

        // Util
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(uuid);
        mockedUtil.when(() -> Util.tabLimit(any(), any())).thenCallRealMethod();
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Bukkit
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        ItemMeta bannerMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        Inventory inventory = mock(Inventory.class);
        mockedBukkit.when(() -> Bukkit.createInventory(any(), Mockito.anyInt(), anyString())).thenReturn(inventory);
        // Flags manager
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pim);
        FlagsManager fm = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(fm);

        asc = new AdminSettingsCommand(ac);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#setup()}.
     */
    @Test
    void testSetup() {
        assertFalse(asc.isOnlyPlayer());
        assertEquals("admin.settings", asc.getPermission());
        assertEquals("commands.admin.settings.parameters", asc.getParameters());
        assertEquals("commands.admin.settings.description", asc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteEmpty() {
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgUnknownPlayer() {
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgKnownPlayerNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgKnownPlayerIslandNotOwner() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgKnownPlayer() {
        assertTrue(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgSpawnNoSpawn() {
        mockedUtil.when(() -> Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "spawn-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteOneArgSpawnSpawn() {
        when(im.getSpawn(any())).thenReturn(Optional.of(island));
        assertTrue(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringNoArgsConsole() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(asc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringArgs() {
        assertFalse(asc.execute(user, "", Arrays.asList("blah", "blah")));
        verify(user, never()).sendMessage("general.success");
    }

    private Flag stubFlag(FlagsManager manager, String id, Flag.Type type) {
        Flag flag = mock(Flag.class);
        when(flag.getID()).thenReturn(id);
        when(flag.getType()).thenReturn(type);
        when(manager.getFlag(id)).thenReturn(Optional.of(flag));
        return flag;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = Flag.Type.class, names = {"SETTING", "PROTECTION"})
    void testInterleavedChangesKeepIslandFlagAndValueTogether(Flag.Type type) {
        FlagsManager manager = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(manager);
        Flag firstFlag = stubFlag(manager, "FIRST", type);
        Flag secondFlag = stubFlag(manager, "SECOND", type);
        when(manager.getFlags()).thenReturn(List.of(firstFlag, secondFlag));
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Island secondIsland = mock(Island.class);
        mockedUtil.when(() -> Util.getUUID("first")).thenReturn(firstId);
        mockedUtil.when(() -> Util.getUUID("second")).thenReturn(secondId);
        when(im.getIsland(world, firstId)).thenReturn(island);
        when(im.getIsland(world, secondId)).thenReturn(secondIsland);
        String firstValue = type == Flag.Type.SETTING ? "protection.panel.flag-item.setting-active" : RanksManager.MEMBER_RANK_REF;
        String secondValue = type == Flag.Type.SETTING ? "protection.panel.flag-item.setting-disabled" : RanksManager.SUB_OWNER_RANK_REF;
        var first = List.of("first", "FIRST", firstValue);
        assertTrue(asc.canExecute(user, "settings", first));
        assertTrue(asc.canExecute(user, "settings", List.of("second", "SECOND", secondValue)));
        assertTrue(asc.execute(user, "settings", first));
        if (type == Flag.Type.SETTING) verify(island).setSettingsFlag(firstFlag, true);
        else verify(island).setFlag(firstFlag, RanksManager.MEMBER_RANK);
        verify(secondIsland, never()).setSettingsFlag(any(), Mockito.anyBoolean());
        verify(secondIsland, never()).setFlag(any(), Mockito.anyInt());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void testInterleavedPanelsKeepCurrentInvocationTarget(boolean templateLoads) {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Island secondIsland = mock(Island.class);
        mockedUtil.when(() -> Util.getUUID("first")).thenReturn(firstId);
        mockedUtil.when(() -> Util.getUUID("second")).thenReturn(secondId);
        when(im.getIsland(world, firstId)).thenReturn(island);
        when(im.getIsland(world, secondId)).thenReturn(secondIsland);
        AdminSettingsCommand command = Mockito.spy(asc);
        Mockito.doNothing().when(command).openLegacyPanel(any(), any());
        assertTrue(command.canExecute(user, "settings", List.of("first")));
        assertTrue(command.canExecute(user, "settings", List.of("second")));
        try (var panels = Mockito.mockStatic(SettingsPanel.class)) {
            panels.when(() -> SettingsPanel.openAdminPanel(command, user, island)).thenReturn(templateLoads);
            assertTrue(command.execute(user, "settings", List.of("first")));
            panels.verify(() -> SettingsPanel.openAdminPanel(command, user, island));
            if (!templateLoads) verify(command).openLegacyPanel(user, island);
            else verify(command, never()).openLegacyPanel(any(), any());
        }
    }

    @Test
    void testExecutionRejectsIslandRemovedAfterValidation() {
        assertTrue(asc.canExecute(user, "settings", List.of("first")));
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        try (var panels = Mockito.mockStatic(SettingsPanel.class)) {
            assertFalse(asc.execute(user, "settings", List.of("first")));
            panels.verifyNoInteractions();
        }
        verify(user, never()).sendMessage("general.success");
    }

    @Test
    void testWorldSettingDoesNotReuseAnotherCallsFlagOrBoolean() {
        FlagsManager manager = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(manager);
        Flag first = stubFlag(manager, "FIRST", Flag.Type.WORLD_SETTING);
        Flag second = stubFlag(manager, "SECOND", Flag.Type.WORLD_SETTING);
        when(manager.getFlags()).thenReturn(List.of(first, second));
        var args = List.of("FIRST", "protection.panel.flag-item.setting-disabled");
        assertTrue(asc.canExecute(user, "settings", args));
        assertTrue(asc.canExecute(user, "settings", List.of("SECOND", "protection.panel.flag-item.setting-active")));
        assertTrue(asc.execute(user, "settings", args));
        verify(first).setSetting(world, false);
        verify(second, never()).setSetting(any(), Mockito.anyBoolean());
    }

    @Test
    void testRemovedFlagDoesNotReportSuccessOrWriteStaleSetting() {
        FlagsManager manager = mock(FlagsManager.class);
        when(plugin.getFlagsManager()).thenReturn(manager);
        Flag flag = stubFlag(manager, "FIRST", Flag.Type.WORLD_SETTING);
        when(manager.getFlags()).thenReturn(List.of(flag));
        var args = List.of("FIRST", "protection.panel.flag-item.setting-active");
        assertTrue(asc.canExecute(user, "settings", args));
        when(manager.getFlag("FIRST")).thenReturn(Optional.empty());
        assertFalse(asc.execute(user, "settings", args));
        verify(flag, never()).setSetting(any(), Mockito.anyBoolean());
        verify(user, never()).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testTabCompleteUserStringListOfStringTwoArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "WORLD_TNT"));
        assertFalse(r.isEmpty());
        assertEquals("WORLD_TNT_DAMAGE", r.get().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testTabCompleteUserStringListOfStringThreeArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "WORLD_TNT", "BEACO"));
        assertFalse(r.isEmpty());
        assertEquals("BEACON", r.get().getFirst());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testTabCompleteUserStringListOfStringFourArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b", "b", "PVP_OVERWORLD", "t"));
        assertFalse(r.isEmpty());
        // TODO - finish this.
    }


    /**
     * Stubs needed to render the templated admin panel.
     */
    private void stubForPanel() {
        when(plugin.getDataFolder()).thenReturn(Path.of("src", "main", "resources").toFile());
        when(user.getTranslation(anyString(), any(String[].class)))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getLocation()).thenReturn(location);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
    }

    /**
     * Without arguments the world form of the templated admin panel opens.
     */
    @Test
    void testExecuteNoArgsOpensWorldPanel() {
        stubForPanel();
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
        assertTrue(asc.execute(user, "", Collections.emptyList()));

        Panel panel = PanelListenerManager.getOpenPanels().get(user.getUniqueId());
        assertInstanceOf(TemplatedPanel.class, panel);
        SettingsPanel sp = (SettingsPanel) panel.getListener().orElseThrow();
        assertEquals(SettingsPanel.TabType.WORLD_SETTING, sp.getActiveTab());
        assertNull(panel.getIsland());
        verify(plugin, never()).logError(anyString());
        TemplateReader.clearPanels();
    }

    /**
     * With a player argument the island form opens for that player's island.
     */
    @Test
    void testExecutePlayerArgOpensIslandPanel() {
        stubForPanel();
        when(island.getWorld()).thenReturn(world);
        assertTrue(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        assertTrue(asc.execute(user, "", Collections.singletonList("tastybento")));

        Panel panel = PanelListenerManager.getOpenPanels().get(user.getUniqueId());
        assertInstanceOf(TemplatedPanel.class, panel);
        SettingsPanel sp = (SettingsPanel) panel.getListener().orElseThrow();
        assertEquals(SettingsPanel.TabType.PROTECTION, sp.getActiveTab());
        assertEquals(island, panel.getIsland());
        TemplateReader.clearPanels();
    }

    /**
     * If the template cannot be loaded, the legacy tabbed panel is shown.
     */
    @Test
    void testExecuteFallsBackToTabbedPanel(@TempDir Path empty) {
        stubForPanel();
        when(plugin.getDataFolder()).thenReturn(empty.toFile());
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
        assertTrue(asc.execute(user, "", Collections.emptyList()));

        verify(plugin).logError("Could not load the admin settings panel template; showing the built-in panel");
        assertInstanceOf(TabbedPanel.class, PanelListenerManager.getOpenPanels().get(user.getUniqueId()));
    }
}
