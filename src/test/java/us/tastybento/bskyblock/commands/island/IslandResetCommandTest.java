/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.managers.island.NewIsland;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, NewIsland.class })
public class IslandResetCommandTest {

    private BSkyBlock plugin;
    private IslandCommand ic;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;

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
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        
        // Parent command has no aliases
        ic = mock(IslandCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.IslandResetCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     * @throws IOException 
     */
    @Test
    public void testNoIsland() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Test the reset command
        // Does not have island
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }
    
    @Test
    public void testNotLeader() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.not-leader");
    }
    
    @Test
    public void testHasTeam() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.reset.must-remove-members");
    }
    
    @Test
    public void testNoResetsLeft() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        
        // Block based on no resets left
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(0);
        
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.reset.none-left");
    }
    
    @Test
    public void testConfirmBeforeReset() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        
        // Test sending confirm immediately
        assertFalse(irc.execute(user, Arrays.asList("confirm")));
    }
    
    @Test
    public void testNoConfirmationRequired() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(oldIsland);
        
        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Reset command, no confirmation required
        assertTrue(irc.execute(user, new ArrayList<>()));
        // Verify that build new island was called and the number of resets left shown
        Mockito.verify(builder).build();
        Mockito.verify(user).sendMessage("commands.island.reset.resets-left", "[number]", "1"); 
    }
    
    @Test
    public void testUnlimitedResets() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(oldIsland);
        
        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        // Test with unlimited resets
        when(s.getResetLimit()).thenReturn(-1);
        
        // Reset
        assertTrue(irc.execute(user, new ArrayList<>()));
        // Verify that build new island was called and the number of resets left shown
        Mockito.verify(builder).build();
        // This should not be shown
        Mockito.verify(user, Mockito.never()).sendMessage("commands.island.reset.resets-left", "[number]", "1"); 
    }
    
    @Test
    public void testConfirmationRequired() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(oldIsland);
        
        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Require confirmation
        when(s.isResetConfirmation()).thenReturn(true);
        when(s.getConfirmationTime()).thenReturn(20);
        
        // Reset
        assertTrue(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.reset.confirm", "[label]", Constants.ISLANDCOMMAND, "[seconds]", String.valueOf(s.getConfirmationTime()));
        
        // Reset confirm
        assertTrue(irc.execute(user, Arrays.asList("confirm")));
        Mockito.verify(builder).build();

    }
    
    @Test
    public void testNewIslandError() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(oldIsland);
        
        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenThrow(new IOException());
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Require no confirmation
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Reset
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.create.unable-create-island");
        

    }
}
