package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class IslandSethomeCommandTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    private UUID uuid;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getOwner()).thenReturn(uuid);
        when(island.onIsland(any())).thenReturn(true);
        when(im.getMaxHomes(eq(island))).thenReturn(1);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(any())).thenReturn(3);
        when(plugin.getIWM()).thenReturn(iwm);

        // Number of homes
        PowerMockito.mockStatic(Util.class);

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#IslandSethomeCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandSethomeCommand() {
        IslandSethomeCommand cmd = new IslandSethomeCommand(ic);
        assertEquals("sethome", cmd.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#setup()}.
     */
    @Test
    public void testSetup() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertEquals("bskyblock.island.sethome", isc.getPermission());
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.sethome.description", isc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        // Player doesn't have an island
        when(im.getIsland(any(), eq(user))).thenReturn(null);

        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNotOnIsland() {
        when(island.onIsland(any())).thenReturn(false);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
        verify(user).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
        verify(user, never()).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHomeSuccess() {
        when(island.getMaxHomes()).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.singletonList("home")));
        assertTrue(isc.execute(user, "island", Collections.singletonList("home")));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMultiHomeTooMany() {
        when(island.getMaxHomes()).thenReturn(3);
        when(im.getNumberOfHomesIfAdded(eq(island), anyString())).thenReturn(4);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.singletonList("13")));
        verify(user).sendMessage(eq("commands.island.sethome.too-many-homes"), eq("[number]"), eq("3"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNether() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherNotAllowed() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.nether.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherConfirmation() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage(eq("commands.island.sethome.nether.confirmation"));
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEnd() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndNotAllowed() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.the-end.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndConfirmation() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage(eq("commands.island.sethome.the-end.confirmation"));
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("0"));
    }
}
