package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
public class IslandTeamTrustCommandTest {

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
    @Mock
    private Player targetPlayer;

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
        // Command
        when(ic.getTopLabel()).thenReturn("island");

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
        // Target player
        when(targetPlayer.getUniqueId()).thenReturn(notUUID);
        when(targetPlayer.getName()).thenReturn("target");
        User.getInstance(targetPlayer);

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
        when(lm.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
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
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoisland() {
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), Mockito.any(UUID.class))).thenReturn(false);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSamePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.trust.trust-in-yourself"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecutePlayerHasRank() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(notUUID));
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("bento")));
        verify(user).sendMessage(eq("commands.island.team.trust.player-already-trusted"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCannottrustSelf() {
        when(pm.getUUID(any())).thenReturn(uuid);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.trust.trust-in-yourself"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCannotAlreadyHasRank() {
        UUID other = UUID.randomUUID();
        when(pm.getUUID(any())).thenReturn(other);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(other));
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.trust.player-already-trusted"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNullIsland() {
        // Can execute
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        // Execute
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.general"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessNoConfirmation() {
        User target = User.getInstance(targetPlayer);
        // Can execute
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));

        // Execute
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("commands.island.team.trust.success"),  eq(TextVariables.NAME), eq("target"));
        verify(island).setRank(target, RanksManager.TRUSTED_RANK);
        verify(targetPlayer).sendMessage("commands.island.team.trust.you-are-trusted");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamTrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessConfirmation() {
        when(s.isInviteConfirmation()).thenReturn(true);
        User target = User.getInstance(targetPlayer);
        // Can execute
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        IslandTeamTrustCommand itl = new IslandTeamTrustCommand(ic);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));

        // Execute
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("commands.island.team.invite.invitation-sent"), eq(TextVariables.NAME), eq("target"));
        // Send message to online player
        verify(targetPlayer).sendMessage(eq("commands.island.team.trust.name-has-invited-you"));
        verify(targetPlayer).sendMessage(eq("commands.island.team.invite.to-accept-or-reject"));
        verify(island, never()).setRank(target, RanksManager.TRUSTED_RANK);
    }

}