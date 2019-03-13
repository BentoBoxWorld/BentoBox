package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private CompositeCommand ic;
    private User user;
    private UUID uuid;
    private IslandsManager im;
    private Island island;
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
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getTranslation(Mockito.anyString())).thenAnswer(i -> i.getArgumentAt(0, String.class));

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

        // IWM friendly name
        iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(Mockito.any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(Mockito.any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(Mockito.any())).thenReturn(3);
        when(plugin.getIWM()).thenReturn(iwm);

        // Number of homes
        PowerMockito.mockStatic(Util.class);
        // 1 home for now
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(1);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#IslandSethomeCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandSethomeCommand() {
        new IslandSethomeCommand(ic);
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
        // Player doesn't have an island and doesn't have a team.
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);

        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNotOnIsland() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        Mockito.verify(user, Mockito.never()).sendMessage("general.errors.no-island");
        Mockito.verify(user).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        Mockito.verify(user, Mockito.never()).sendMessage("general.errors.no-island");
        Mockito.verify(user, Mockito.never()).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.sethome.home-set");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoMultiHome() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.singletonList("3")));
        Mockito.verify(user).sendMessage("general.errors.no-permission");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMultiHomeSuccess() {
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.singletonList("3")));
        Mockito.verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMultiHomeTooHigh() {
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.singletonList("13")));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.sethome.num-homes"), Mockito.eq("[number]"), Mockito.eq("5"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMultiHomeTooLow() {
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.singletonList("-3")));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.sethome.num-homes"), Mockito.eq("[number]"), Mockito.eq("5"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMultiHomeNAN() {
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.singletonList("six")));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.sethome.num-homes"), Mockito.eq("[number]"), Mockito.eq("5"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNether() {
        when(iwm.isNether(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherNotAllowed() {
        when(iwm.isNether(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.sethome.nether.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherConfirmation() {
        when(iwm.isNether(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(true);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendRawMessage(Mockito.eq("commands.island.sethome.nether.confirmation"));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.confirmation.confirm"), Mockito.eq("[seconds]"), Mockito.eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEnd() {
        when(iwm.isEnd(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndNotAllowed() {
        when(iwm.isEnd(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(false);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.sethome.the-end.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndConfirmation() {
        when(iwm.isEnd(Mockito.any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(true);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        Mockito.verify(user).sendRawMessage(Mockito.eq("commands.island.sethome.the-end.confirmation"));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.confirmation.confirm"), Mockito.eq("[seconds]"), Mockito.eq("0"));
    }
}
