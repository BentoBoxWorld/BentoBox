package world.bentobox.bentobox.api.commands.island;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
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
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class IslandBanCommandTest {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;
    @Mock
    private Addon addon;
    private IslandBanCommand ibc;
    @Mock
    private Player targetPlayer;

    private RanksManager rm;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(-1);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);


        // Server and Plugin Manager for events
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Addon
        when(ic.getAddon()).thenReturn(addon);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Target bill - default target. Non Op, online, no ban prevention permission
        UUID uuid = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(uuid);
        when(targetPlayer.getName()).thenReturn("bill");
        when(targetPlayer.getUniqueId()).thenReturn(uuid);
        when(targetPlayer.isOp()).thenReturn(false);
        when(targetPlayer.isOnline()).thenReturn(true);
        when(targetPlayer.hasPermission(anyString())).thenReturn(false);
        User.getInstance(targetPlayer);

        // Ranks Manager
        rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

        // Island Ban Command
        ibc = new IslandBanCommand(ic);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandBanCommand#execute(User, String, List)}.
     */
    // Island ban command by itself

    // *** Error conditions ***
    // Ban without an island
    // Ban as not an owner
    // Ban unknown user
    // Ban self
    // Ban team mate
    // Ban someone you have already banned
    // Ban an Op

    // *** Working conditions ***
    // Ban offline user
    // Ban online user

    @Test
    public void testNoArgs() {
        assertFalse(ibc.canExecute(user, ibc.getLabel(), new ArrayList<>()));
    }

    @Test
    public void testNoIsland() {
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testTooLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    @Test
    public void testUnknownUser() {
        when(pm.getUUID(Mockito.anyString())).thenReturn(null);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "bill");
    }

    @Test
    public void testBanSelf() {
        when(pm.getUUID(anyString())).thenReturn(uuid);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.cannot-ban-yourself");
    }

    @Test
    public void testBanTeamMate() {
        UUID teamMate = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(teamMate);
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(teamMate);
        when(im.getMembers(any(), any())).thenReturn(members);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.cannot-ban-member");
    }

    @Test
    public void testBanAlreadyBanned() {
        UUID bannedUser = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(bannedUser);
        when(island.isBanned(eq(bannedUser))).thenReturn(true);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.player-already-banned");
    }

    @Test
    public void testBanOp() {
        when(targetPlayer.isOp()).thenReturn(true);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.cannot-ban");
    }

    @Test
    public void testBanOnlineNoBanPermission() {
        when(targetPlayer.hasPermission(anyString())).thenReturn(true);
        User.getInstance(targetPlayer);

        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("billy")));
        verify(user).sendMessage("commands.island.ban.cannot-ban");
    }

    @Test
    public void testBanOfflineUserSuccess() {
        when(targetPlayer.isOnline()).thenReturn(false);
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Allow adding to ban list
        when(island.ban(any(), any())).thenReturn(true);
        // Run execute
        assertTrue(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.player-banned", "[name]", "bill");
        verify(targetPlayer).sendMessage("commands.island.ban.owner-banned-you");
    }

    @Test
    public void testBanOnlineUserSuccess() {
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Allow adding to ban list
        when(island.ban(any(), any())).thenReturn(true);

        assertTrue(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.player-banned", "[name]", "bill");
        verify(targetPlayer).sendMessage("commands.island.ban.owner-banned-you");
    }

    @Test
    public void testCancelledBan() {
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Disallow adding to ban list - event cancelled
        when(island.ban(any(), any())).thenReturn(false);

        assertFalse(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user, never()).sendMessage("commands.island.ban.player-banned", TextVariables.NAME, targetPlayer.getName());
        verify(targetPlayer, never()).sendMessage("commands.island.ban.owner-banned-you");
    }

    @Test
    public void testTabCompleteNoIsland() {
        // No island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
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

        String[] names = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};
        Map<UUID, String> online = new HashMap<>();

        Set<UUID> banned = new HashSet<>();
        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < names.length; j++) {
            Player p = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p.getUniqueId()).thenReturn(uuid);
            when(p.getName()).thenReturn(names[j]);
            online.put(uuid, names[j]);
            // Ban the first 3 players
            if (j < 3) {
                banned.add(uuid);
            }
            onlinePlayers.add(p);
        }

        when(island.isBanned(any(UUID.class))).thenAnswer((Answer<Boolean>) invocation -> banned.contains(invocation.getArgument(0, UUID.class)));
        // Create the names
        when(pm.getName(any(UUID.class))).then((Answer<String>) invocation -> online.getOrDefault(invocation.getArgument(0, UUID.class), "tastybento"));

        // Return a set of online players
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

        // Set up the user
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Player player = mock(Player.class);
        // Player can see every other player except Ian
        when(player.canSee(any(Player.class))).thenAnswer((Answer<Boolean>) invocation -> {
            Player p = invocation.getArgument(0, Player.class);
            return !p.getName().equals("ian");
        });
        when(user.getPlayer()).thenReturn(player);

        // Get the tab-complete list with no argument
        Optional<List<String>> result = ibc.tabComplete(user, "", new LinkedList<>());
        assertFalse(result.isPresent());

        // Get the tab-complete list with one argument
        LinkedList<String> args = new LinkedList<>();
        args.add("");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expectedNames = {"dave", "ed", "frank", "freddy", "george", "harry", "joe"};
        assertTrue(Arrays.equals(expectedNames, r.toArray()));

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("d");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expectedName = {"dave"};
        assertTrue(Arrays.equals(expectedName, r.toArray()));

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("fr");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expected = {"frank", "freddy"};
        assertTrue(Arrays.equals(expected, r.toArray()));
    }
}
