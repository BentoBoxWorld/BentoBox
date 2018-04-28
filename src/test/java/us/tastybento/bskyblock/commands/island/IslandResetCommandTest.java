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
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
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
@PrepareForTest({BSkyBlock.class, NewIsland.class })
public class IslandResetCommandTest {

    private static BSkyBlock plugin;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        /*
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        */
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.IslandResetCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     * @throws IOException 
     */
    @Test
    public void testExecuteUserListOfString() throws IOException {
        Settings s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(plugin.getSettings()).thenReturn(s);
        
        Player p = mock(Player.class);
        User user = mock(User.class, Mockito.withSettings().verboseLogging());
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        
        IslandCommand ic = mock(IslandCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        
        IslandResetCommand irc = new IslandResetCommand(ic);
        
        // No island
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.inTeam(Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);
        
        // Test the reset command
        // Does not have island
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.no-island");
        
        // Now has island, but is not the leader
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.not-leader");
                
        // Now is owner, but still has team
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.reset.must-remove-members");
        
        // Now has no team
        when(pm.inTeam(Mockito.eq(uuid))).thenReturn(false);
        
        // Block based on no resets left
        when(s.getResetLimit()).thenReturn(1);
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(0);
        assertFalse(irc.execute(user, new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.reset.none-left");
        
        // Give the user some resets
        when(pm.getResetsLeft(Mockito.eq(uuid))).thenReturn(1);
        
        // No confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        
        // Old island
        Island oldIsland = mock(Island.class);
        when(im.getIsland(Mockito.eq(uuid))).thenReturn(oldIsland);
        
        // Mock up NewIsland
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Reset
        assertTrue(irc.execute(user, new ArrayList<>()));
        Mockito.verify(builder).build();
        Mockito.verify(user).sendMessage("commands.island.reset.resets-left", "[number]", "1"); 
        
        
        
    }
}
