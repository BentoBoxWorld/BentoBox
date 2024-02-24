package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;

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
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
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
    @Mock
    private @Nullable Island island;
    @Mock
    private @NonNull Location location;
    private AdminTeamDisbandCommand itl;

    /**
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
        while (notUUID.equals(uuid)) {
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
        when(island.getOwner()).thenReturn(uuid);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid, notUUID));
        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        when(im.getOwnedIslands(any(), eq(uuid))).thenReturn(Set.of(island));
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
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Plugin Manager
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Online players
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getOnlinePlayerList(user)).thenReturn(List.of("tastybento", "BONNe"));
        when(Util.translateColorCodes(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // DUT
        itl = new AdminTeamDisbandCommand(ac);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        String[] name = { "tastybento" };
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecutePlayerNotInTeam() {
        when(Util.getUUID("tastybento")).thenReturn(notUUID);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.player-is-not-owner", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamDisbandCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteDisbandNoIsland() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(im.getOwnedIslands(any(), eq(uuid))).thenReturn(Set.of());
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteSuccess() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getName(uuid)).thenReturn("tastybento");
        // Members
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid, notUUID));


        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        this.testCanExecuteSuccess();
        assertTrue(itl.execute(user, itl.getLabel(), List.of("tastybento")));
        verify(im, never()).removePlayer(island, uuid);
        verify(im).removePlayer(island, notUUID);
        verify(user).sendMessage("commands.admin.team.disband.success", TextVariables.NAME, "tastybento");
        verify(p).sendMessage("commands.admin.team.disband.disbanded");
        verify(p2).sendMessage("commands.admin.team.disband.disbanded");
        // 2 + 1
        verify(pim, times(3)).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabCompleteNoArgs() {
        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        Optional<List<String>> list = itl.tabComplete(user, "", List.of(""));
        assertTrue(list.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabCompleteOneArg() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getName(uuid)).thenReturn("tastybento");

        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        Optional<List<String>> list = itl.tabComplete(user, "", List.of("tasty"));
        assertTrue(list.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabCompleteTwoArgs() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getName(uuid)).thenReturn("tastybento");

        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        Optional<List<String>> list = itl.tabComplete(user, "", List.of("tastybento", "1"));
        assertTrue(list.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabCompleteThreeArgs() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getName(uuid)).thenReturn("tastybento");

        AdminTeamDisbandCommand itl = new AdminTeamDisbandCommand(ac);
        Optional<List<String>> list = itl.tabComplete(user, "", List.of("tastybento", "1,2,3", "ddd"));
        assertFalse(list.isEmpty());
    }

}
