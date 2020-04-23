package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminTeamKickCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private World world;
    @Mock
    private PluginManager pim;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
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
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.isOwner(any(),any())).thenReturn(true);
        when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecutePlayerNotInTeam() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(new HashSet<>());
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.admin.team.kick.not-in-team"));
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)} .
     */
    @Test
    public void testExecuteKickOwner() {
        when(im.inTeam(any(), any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        when(pm.getUUID(any())).thenReturn(notUUID);

        when(is.getOwner()).thenReturn(notUUID);

        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.admin.team.kick.cannot-kick-owner"));
        verify(is).showMembers(any());
        verify(im, never()).removePlayer(eq(world), eq(notUUID));
        verify(pm, never()).clearHomeLocations(eq(world), eq(notUUID));
        verify(user, never()).sendMessage(eq("commands.admin.team.kick.success"), anyString(), anyString(), anyString(), anyString());
        verify(pim, never()).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamKickCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecute() {
        when(im.inTeam(any(), any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        String name = "tastybento";
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(any())).thenReturn(name);

        when(is.getOwner()).thenReturn(uuid);

        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList(name)));
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList(name)));
        verify(im).removePlayer(eq(world), eq(notUUID));
        verify(pm).clearHomeLocations(eq(world), eq(notUUID));
        verify(user).sendMessage(eq("commands.admin.team.kick.success"), eq(TextVariables.NAME), eq(name), eq("[owner]"), anyString());
        // Offline so event will be called four time
        verify(pim, times(4)).callEvent(any());
    }

}
