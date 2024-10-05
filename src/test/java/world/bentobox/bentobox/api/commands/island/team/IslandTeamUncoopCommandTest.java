package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
public class IslandTeamUncoopCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Settings s;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        pm = mock(PlayersManager.class);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNoisland() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSamePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(uuid);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.uncoop.cannot-uncoop-yourself"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerHasRank() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.inTeam(any(), any())).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));
        when(island.inTeam(notUUID)).thenReturn(true);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("bento")));
        verify(user).sendMessage(eq("commands.island.team.uncoop.cannot-uncoop-member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamUncoopCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteCoolDownActive() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        IslandTeamUncoopCommand itl = new IslandTeamUncoopCommand(ic);
        String[] name = {"tastybento"};
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
    }

    @Test
    public void testTabCompleteNoIsland() {
        // No island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        IslandTeamUncoopCommand ibc = new IslandTeamUncoopCommand(ic);
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
    public void testTabCompleteNoArgument() {

        Map<UUID, Integer> map = new HashMap<>();
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);

        when(island.getMembers()).thenReturn(map);
        // Return a set of players
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george",
                "harry", "ian", "joe");

        IslandTeamUncoopCommand ibc = new IslandTeamUncoopCommand(ic);
        // Get the tab-complete list with no argument
        Optional<List<String>> result = ibc.tabComplete(user, "", new LinkedList<>());
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().toList();
        // Compare the expected with the actual
        String[] expectedNames = { "adam", "ben", "cara" };

        assertTrue(Arrays.equals(expectedNames, r.toArray()));

    }

    @Test
    public void testTabCompleteWithArgument() {

        Map<UUID, Integer> map = new HashMap<>();
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        map.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);

        when(island.getMembers()).thenReturn(map);
        // Return a set of players
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george",
                "harry", "ian", "joe");

        IslandTeamUncoopCommand ibc = new IslandTeamUncoopCommand(ic);
        // Get the tab-complete list with argument
        LinkedList<String> args = new LinkedList<>();
        args.add("c");
        Optional<List<String>> result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().toList();
        // Compare the expected with the actual
        String[] expectedNames = { "cara" };

        assertTrue(Arrays.equals(expectedNames, r.toArray()));

    }
}
