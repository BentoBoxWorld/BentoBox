package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
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
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminTeamDisbandCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private Player p;
    @Mock
    private Player p2;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private PluginManager pim;
    private UUID notUUID;

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
        // Sometimes use withSettings().verboseLogging()
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
        // Set up users
        when(p.getUniqueId()).thenReturn(uuid);
        when(p2.getUniqueId()).thenReturn(notUUID);
        User.getInstance(p);
        User.getInstance(p2);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.isOwner(any(),any())).thenReturn(true);
        when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Plugin Manager
        when(Bukkit.getPluginManager()).thenReturn(pim);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecutePlayerNotInTeam() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(new HashSet<>());
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage(eq("general.errors.not-in-team"));
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteDisbandNotOwner() {
        when(im.inTeam(any(), any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);

        when(im.getOwner(any(), eq(notUUID))).thenReturn(uuid);
        when(pm.getName(any())).thenReturn("owner");

        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("commands.admin.team.disband.use-disband-owner", "[owner]", "owner");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(any(), any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(any())).thenReturn(name[0]);
        // Owner
        when(im.getOwner(any(), eq(notUUID))).thenReturn(notUUID);
        // Members
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(notUUID);
        when(im.getMembers(any(), any())).thenReturn(members);

        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(im, never()).setLeaveTeam(any(), eq(notUUID));
        verify(im).setLeaveTeam(any(), eq(uuid));
        verify(user).sendMessage("commands.admin.team.disband.success", TextVariables.NAME, name[0]);
        verify(p).sendMessage("commands.admin.team.disband.disbanded");
        verify(p2).sendMessage("commands.admin.team.disband.disbanded");
        verify(pim, times(3)).callEvent(any());
    }
}
