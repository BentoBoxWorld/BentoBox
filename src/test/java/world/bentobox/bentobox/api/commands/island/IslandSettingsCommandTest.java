package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.panels.settings.WorldProtectionInfoTab;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link IslandSettingsCommand}, in particular what is shown to a
 * player who is not on an island.
 *
 * @author tastybento
 */
class IslandSettingsCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private LocalesManager testLm;

    private IslandSettingsCommand isc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        User.setPlugin(plugin);
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        when(user.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));

        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        when(plugin.getIslands()).thenReturn(im);
        when(testLm.get(any(User.class), anyString())).thenAnswer(i -> i.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(testLm);

        isc = new IslandSettingsCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * A player standing on an island gets the island's protection and settings tabs.
     */
    @Test
    void testBuildPanelWithIsland() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getWorld()).thenReturn(world);

        assertTrue(isc.canExecute(user, "settings", Collections.emptyList()));
        Map<Integer, Tab> tabs = isc.buildPanel(user).getTabs();

        assertEquals(2, tabs.size());
        assertEquals(Type.PROTECTION, ((SettingsTab) tabs.get(1)).getType());
        assertEquals(Type.SETTING, ((SettingsTab) tabs.get(2)).getType());
        assertEquals(island, isc.buildPanel(user).getIsland());
    }

    /**
     * With no island the command still runs - it is the moment a player most
     * wants to know what the rules are where they are standing.
     */
    @Test
    void testCanExecuteWithoutIsland() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);

        assertTrue(isc.canExecute(user, "settings", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
    }

    /**
     * With no island only the world's protection flags are shown: settings flags
     * are an island concept and there is no island to configure.
     */
    @Test
    void testBuildPanelWithoutIslandShowsOnlyWorldProtection() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        isc.canExecute(user, "settings", Collections.emptyList());

        var tpb = isc.buildPanel(user);
        Map<Integer, Tab> tabs = tpb.getTabs();

        assertEquals(1, tabs.size());
        assertInstanceOf(WorldProtectionInfoTab.class, tabs.get(1));
        assertEquals(world, tpb.getWorld());
        // No island is set on the panel
        assertNull(tpb.getIsland());
    }

    /**
     * The command opens the templated settings panel.
     */
    @Test
    void testExecuteOpensTemplatedPanel() {
        when(plugin.getDataFolder()).thenReturn(Path.of("src", "main", "resources").toFile());
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(user.getTranslation(anyString(), any(String[].class))).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.getWorld()).thenReturn(world);
        isc.canExecute(user, "settings", Collections.emptyList());

        assertTrue(isc.execute(user, "settings", Collections.emptyList()));

        assertInstanceOf(TemplatedPanel.class, PanelListenerManager.getOpenPanels().get(user.getUniqueId()));
        verify(plugin, never()).logError(anyString());
        TemplateReader.clearPanels();
    }

    /**
     * If the template cannot be loaded, the legacy tabbed panel is shown so that players still
     * get their settings.
     */
    @Test
    void testExecuteFallsBackToTabbedPanel(@TempDir Path empty) {
        when(plugin.getDataFolder()).thenReturn(empty.toFile());
        when(plugin.getFlagsManager()).thenReturn(fm);
        Settings settings = mock(Settings.class);
        when(settings.getPanelFillerMaterial()).thenReturn(Material.BLACK_STAINED_GLASS_PANE);
        when(plugin.getSettings()).thenReturn(settings);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        isc.canExecute(user, "settings", Collections.emptyList());

        assertTrue(isc.execute(user, "settings", Collections.emptyList()));

        verify(plugin).logError("Could not load the settings panel template; showing the built-in panel");
        assertInstanceOf(TabbedPanel.class, PanelListenerManager.getOpenPanels().get(user.getUniqueId()));
    }
}
