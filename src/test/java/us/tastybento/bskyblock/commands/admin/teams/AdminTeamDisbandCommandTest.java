package us.tastybento.bskyblock.commands.admin.teams;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.AdminCommand;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class })
public class AdminTeamDisbandCommandTest {

    private BSkyBlock plugin;
    private AdminCommand ac;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
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
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(AdminCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        
        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        World world = mock(World.class);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(),Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team 
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
    }


    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertFalse(itl.execute(user, new ArrayList<>()));
        // Show help
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.unknown-player"));
    }
        
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecutePlayerNotInTeam() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(new HashSet<>());
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-in-team"));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteDisbandNotLeader() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        
        when(im.getTeamLeader(Mockito.any(), Mockito.eq(notUUID))).thenReturn(uuid);
        when(pm.getName(Mockito.any())).thenReturn("leader");
        
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.team.disband.use-disband-leader", "[leader]", "leader");
    }
    
    /**
     * Test method for {@link us.us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        // Leader
        when(im.getTeamLeader(Mockito.any(), Mockito.eq(notUUID))).thenReturn(notUUID);
        // Members
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertTrue(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(im, Mockito.never()).setLeaveTeam(Mockito.any(), Mockito.eq(notUUID));
        Mockito.verify(im).setLeaveTeam(Mockito.any(), Mockito.eq(uuid));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }
    
}
