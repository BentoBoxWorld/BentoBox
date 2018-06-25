/**
 * 
 */
package us.tastybento.bskyblock.commands.island.teams;

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

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class })
public class IslandTeamLeaveCommandTest {

    private IslandCommand ic;
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
        BSkyBlock plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(s.getResetLimit()).thenReturn(3);
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
        ic = mock(IslandCommand.class);
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
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamLeaveCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-team"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamLeaveCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteInTeamLeader() {
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.leave.cannot-leave"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamLeaveCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Add a team leader - null
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, new ArrayList<>()));
        Mockito.verify(im).setLeaveTeam(Mockito.any(), Mockito.eq(uuid));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamLeaveCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
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
        assertFalse(itl.execute(user, new ArrayList<>()));
        // Confirmation required
        Mockito.verify(user).sendMessage(Mockito.eq("general.confirm"), Mockito.eq("[seconds]"), Mockito.eq("0"));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamLeaveCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
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
        assertTrue(itl.execute(user, new ArrayList<>()));
        Mockito.verify(im).setLeaveTeam(Mockito.any(), Mockito.eq(uuid));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
        
        Mockito.verify(enderChest).clear();
        Mockito.verify(inv).clear();
    }
}
