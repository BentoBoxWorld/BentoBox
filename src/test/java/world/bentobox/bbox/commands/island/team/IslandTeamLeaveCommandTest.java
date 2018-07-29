/**
 * 
 */
package world.bentobox.bbox.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.Settings;
import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;
import world.bentobox.bbox.managers.CommandsManager;
import world.bentobox.bbox.managers.IslandWorldManager;
import world.bentobox.bbox.managers.IslandsManager;
import world.bentobox.bbox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamLeaveCommandTest {

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private IslandWorldManager iwm;
    private Player player;

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
        s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);

        when(plugin.getSettings()).thenReturn(s);

        // Player
        player = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island World Manager
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
    }

    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-team"));
    }
    
    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteInTeamLeader() {
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.leave.cannot-leave"));
    }
    
    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Add a team leader - null
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(im).setLeaveTeam(Mockito.any(), Mockito.eq(uuid));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }
    
    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteWithConfirmation() {
        when(s.isLeaveConfirmation()).thenReturn(true);
        // 3 second timeout
        when(s.getLeaveWait()).thenReturn(3L);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Add a team leader - null
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Confirmation required
        Mockito.verify(user).sendMessage(Mockito.eq("general.confirm"), Mockito.eq("[seconds]"), Mockito.eq("0"));
    }

    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteTestResets() {
        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Add a team leader - null
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(null);
        
        // Require resets
        when(iwm.isOnLeaveResetEnderChest(Mockito.any())).thenReturn(true);
        Inventory enderChest = mock(Inventory.class);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(iwm.isOnLeaveResetInventory(Mockito.any())).thenReturn(true);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(iwm.isOnLeaveResetMoney(Mockito.any())).thenReturn(true);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(im).setLeaveTeam(Mockito.any(), Mockito.eq(uuid));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
        
        Mockito.verify(enderChest).clear();
        Mockito.verify(inv).clear();
    }
}
