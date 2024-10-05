package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
public class IslandDeletehomeCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;

    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private IslandDeletehomeCommand idh;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getWorld()).thenReturn(world);
        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.onIsland(any())).thenReturn(true);
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(im.getIsland(world, user)).thenReturn(island);
        when(im.getIslands(world, uuid)).thenReturn(List.of(island));
        @NotNull
        Map<String, Location> homeMap = new HashMap<>();
        homeMap.put("Home", null);
        homeMap.put("Home2", null);
        homeMap.put("Home3", null);
        homeMap.put("Home4", null);
        when(island.getHomes()).thenReturn(homeMap);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(any())).thenReturn(3);

        idh = new IslandDeletehomeCommand(ic);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#IslandDeletehomeCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandDeletehomeCommand() {
        assertEquals("deletehome", idh.getLabel());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(idh.isOnlyPlayer());
        assertEquals("commands.island.deletehome.parameters", idh.getParameters());
        assertEquals("commands.island.deletehome.description", idh.getDescription());
        assertEquals("permission.island.deletehome", idh.getPermission());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteHelp() {
        idh.canExecute(user, "label", List.of());
        verify(user).sendMessage("commands.help.header","[label]","BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.getIsland(any(), eq(user))).thenReturn(null);
        assertFalse(idh.canExecute(user, "label", List.of("something")));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(user)).thenReturn(RanksManager.COOP_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(idh.canExecute(user, "label", List.of("something")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownHome() {
        when(island.getHomes()).thenReturn(Map.of("home", location));

        when(im.isHomeLocation(eq(island), anyString())).thenReturn(false);

        assertFalse(idh.execute(user, "label", List.of("something")));
        verify(user).sendMessage("commands.island.go.unknown-home");
        verify(user).sendMessage("commands.island.sethome.homes-are");
        verify(user).sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, "home");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        when(island.getHomes()).thenReturn(Map.of("home", location));
        when(im.isHomeLocation(eq(island), anyString())).thenReturn(true);
        assertTrue(idh.execute(user, "label", List.of("home")));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "10");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        when(island.getHomes()).thenReturn(Map.of("home", location));
        Optional<List<String>> list = idh.tabComplete(user, "label", List.of("hom"));
        assertTrue(list.isPresent());
        assertEquals("home", list.get().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringNothing() {
        when(island.getHomes()).thenReturn(Map.of("home", location));
        Optional<List<String>> list = idh.tabComplete(user, "label", List.of("f"));
        assertTrue(list.isPresent());
        assertTrue(list.get().isEmpty());
    }

}
