package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamUntrustCommandTest {

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Settings s;
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

        // Settings
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
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        when(im.isOwner(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        island = mock(Island.class);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        pm = mock(PlayersManager.class);

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
        when(plugin.getIWM()).thenReturn(iwm);

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNoisland() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(any())).thenReturn(RanksManager.OWNER_RANK);
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSamePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(uuid);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.untrust.cannot-untrust-yourself"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerHasRank() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(notUUID));
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bento")));
        verify(user).sendMessage(eq("commands.island.team.untrust.cannot-untrust-member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUntrustCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteCoolDownActive() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        IslandTeamUntrustCommand itl = new IslandTeamUntrustCommand(ic);
        String[] name = {"tastybento"};
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
    }

    @Test
    public void testTabCompleteNoIsland() {
        // No island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        IslandTeamUntrustCommand ibc = new IslandTeamUntrustCommand(ic);
        // Set up the user
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        // Get the tab-complete list with one argument
        LinkedList<String> args = new LinkedList<>();
        args.add("");
        Optional<List<String>> result = ibc.tabComplete(user, "", args);
        assertFalse(result.isPresent());

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("d");
        result = ibc.tabComplete(user, "", args);
        assertFalse(result.isPresent());

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("fr");
        result = ibc.tabComplete(user, "", args);
        assertFalse(result.isPresent());
    }

    @Test
    public void testTabComplete() {

        Builder<UUID> memberSet = new ImmutableSet.Builder<>();
        for (int j = 0; j < 11; j++) {
            memberSet.add(UUID.randomUUID());
        }

        when(island.getMemberSet()).thenReturn(memberSet.build());
        // Return a set of players
        PowerMockito.mockStatic(Bukkit.class);
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe");
        when(island.getRank(any(User.class))).thenReturn(
                RanksManager.TRUSTED_RANK,
                RanksManager.TRUSTED_RANK,
                RanksManager.TRUSTED_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK
                );

        IslandTeamUntrustCommand ibc = new IslandTeamUntrustCommand(ic);
        // Get the tab-complete list with no argument
        Optional<List<String>> result = ibc.tabComplete(user, "", new LinkedList<>());
        assertFalse(result.isPresent());

        // Get the tab-complete list with no argument
        LinkedList<String> args = new LinkedList<>();
        args.add("");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expectedNames = {"adam", "ben", "cara"};

        assertTrue(Arrays.equals(expectedNames, r.toArray()));

    }
}
