/**
 * 
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Test for island go command
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class IslandGoCommandTest {
    private CompositeCommand ic;
    private User user;
    private IslandsManager im;
    private Island island;

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
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
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
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
        
        // Number of homes
        PowerMockito.mockStatic(Util.class);
        // 1 home for now
        when(Util.getPermValue(Mockito.any(Player.class), Mockito.anyString(), Mockito.anyInt())).thenReturn(1);
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoArgsNoIsland() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(null);
        IslandGoCommand igc = new IslandGoCommand(ic);
        assertFalse(igc.execute(user, igc.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.no-island");
        
    }
    
    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoArgs() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        IslandGoCommand igc = new IslandGoCommand(ic);
        assertTrue(igc.execute(user, igc.getLabel(), new ArrayList<>()));
    }
    
    /**
     * Test method for .
     */
    @Test
    public void testExecuteNoArgsMultipleHomes() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(Util.getPermValue(Mockito.any(Player.class), Mockito.anyString(), Mockito.anyInt())).thenReturn(3);
        IslandGoCommand igc = new IslandGoCommand(ic);
        assertTrue(igc.execute(user, igc.getLabel(), new ArrayList<>()));
    }
    
    /**
     * Test method for .
     */
    @Test
    public void testExecuteArgs1MultipleHomes() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(Util.getPermValue(Mockito.any(Player.class), Mockito.anyString(), Mockito.anyInt())).thenReturn(3);
        IslandGoCommand igc = new IslandGoCommand(ic);
        List<String> args = new ArrayList<>();
        args.add("1");
        assertTrue(igc.execute(user, igc.getLabel(), args));
    }
    
    /**
     * Test method for .
     */
    @Test
    public void testExecuteArgs2MultipleHomes() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(Util.getPermValue(Mockito.any(Player.class), Mockito.anyString(), Mockito.anyInt())).thenReturn(3);
        IslandGoCommand igc = new IslandGoCommand(ic);
        List<String> args = new ArrayList<>();
        args.add("2");
        assertTrue(igc.execute(user, igc.getLabel(), args));
        Mockito.verify(user).sendMessage("commands.island.go.tip", TextVariables.LABEL, "island");
    }

    
    /**
     * Test method for .
     */
    @Test
    public void testExecuteArgsJunkMultipleHomes() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(Util.getPermValue(Mockito.any(Player.class), Mockito.anyString(), Mockito.anyInt())).thenReturn(3);
        IslandGoCommand igc = new IslandGoCommand(ic);
        List<String> args = new ArrayList<>();
        args.add("sdfsdf");
        assertTrue(igc.execute(user, igc.getLabel(), args));
    }
}
