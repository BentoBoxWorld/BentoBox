package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamCoopCommandTest {

    @Mock
    private IslandTeamCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Settings s;
    @Mock
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

        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(4);

        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        // Island
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(island.getMemberSet(anyInt(), any(Boolean.class))).thenReturn(ImmutableSet.of(uuid));
        when(im.getIsland(any(), Mockito.any(User.class))).thenReturn(island);
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.getMaxCoopSize(any())).thenReturn(4);
        when(plugin.getIWM()).thenReturn(iwm);

        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

    }

    @After
    public void tearDown() {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoisland() {
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), Mockito.any(UUID.class))).thenReturn(false);
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSamePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.coop.cannot-coop-yourself"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecutePlayerHasRank() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(notUUID));
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bento")));
        verify(user).sendMessage(eq("commands.island.team.coop.already-has-rank"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCannotCoopSelf() {
        when(pm.getUUID(any())).thenReturn(uuid);
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.coop.cannot-coop-yourself"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCannotAlreadyHasRank() {
        UUID other = UUID.randomUUID();
        when(pm.getUUID(any())).thenReturn(other);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(other));
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.coop.already-has-rank"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSuccess() {
        UUID other = UUID.randomUUID();
        when(pm.getUUID(any())).thenReturn(other);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNullIsland() {
        // Can execute
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        // Execute
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.general"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamCoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccess() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(notUUID);
        User target = User.getInstance(p);
        // Can execute
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        IslandTeamCoopCommand itl = new IslandTeamCoopCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        // Execute
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.island.team.coop.success",  TextVariables.NAME, null);
        verify(island).setRank(target, RanksManager.COOP_RANK);
    }
}
