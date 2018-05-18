/**
 * 
 */
package us.tastybento.bskyblock.commands.island.teams;

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
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class })
public class IslandTeamKickCommandTest {

    private BSkyBlock plugin;
    private IslandCommand ic;
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
        ic = mock(IslandCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
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
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-team"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNotTeamLeader() {
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(notUUID);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-leader"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, new ArrayList<>()));
        // Show help
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.unknown-player"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteSamePlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(uuid);
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.kick.cannot-kick"));
    }
    
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteDifferentPlayerNotInTeam() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(new HashSet<>());
        assertFalse(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-in-team"));
    }
       
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isKickConfirmation()).thenReturn(false);
        
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        
        Set<UUID> members = new HashSet<>();
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, Arrays.asList(name)));
        Mockito.verify(im).removePlayer(Mockito.any(), Mockito.eq(notUUID));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.teams.IslandTeamKickCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteWithConfirmation() {
        when(s.isKickConfirmation()).thenReturn(true);
        
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        
        Set<UUID> members = new HashSet<>();
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, Arrays.asList(name)));
        // Confirmation required
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.kick.type-again"));
    }

}
