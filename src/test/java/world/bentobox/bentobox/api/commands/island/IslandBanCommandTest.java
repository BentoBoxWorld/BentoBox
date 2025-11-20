package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandBanCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Addon addon;
    private IslandBanCommand ibc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getDisplayName()).thenReturn("&Ctastybento");
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(-1);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));
        when(plugin.getPlayers()).thenReturn(pm);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

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
        when(mockPlayer.getName()).thenReturn("bill");
        when(mockPlayer.getDisplayName()).thenReturn("&Cbill");
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.isOp()).thenReturn(false);
        when(mockPlayer.isOnline()).thenReturn(true);
        when(mockPlayer.hasPermission(anyString())).thenReturn(false);
        User.getInstance(mockPlayer);

        // Island Ban Command
        ibc = new IslandBanCommand(ic);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandBanCommand#execute(User, String, List)}.
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
        //when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testTooLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
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
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid, teamMate));
        when(island.inTeam(teamMate)).thenReturn(true);
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
        when(mockPlayer.isOp()).thenReturn(true);
        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.cannot-ban");
    }

    @Test
    public void testBanOnlineNoBanPermission() {
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        User.getInstance(mockPlayer);

        assertFalse(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("billy")));
        verify(user).sendMessage("commands.island.ban.cannot-ban");
    }

    @Test
    public void testBanOfflineUserSuccess() {
        when(mockPlayer.isOnline()).thenReturn(false);
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Allow adding to ban list
        when(island.ban(any(), any())).thenReturn(true);
        // Run execute
        assertTrue(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.player-banned", TextVariables.NAME, "bill", TextVariables.DISPLAY_NAME, "&Cbill");
        checkSpigotMessage("commands.island.ban.owner-banned-you");
    }

    @Test
    public void testBanOnlineUserSuccess() {
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Allow adding to ban list
        when(island.ban(any(), any())).thenReturn(true);

        assertTrue(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.ban.player-banned", TextVariables.NAME, "bill",
                TextVariables.DISPLAY_NAME, "&Cbill");
        checkSpigotMessage("commands.island.ban.owner-banned-you");
    }

    @Test
    public void testCancelledBan() {
        assertTrue(ibc.canExecute(user, ibc.getLabel(), Collections.singletonList("bill")));

        // Disallow adding to ban list - event cancelled
        when(island.ban(any(), any())).thenReturn(false);

        assertFalse(ibc.execute(user, ibc.getLabel(), Collections.singletonList("bill")));
        verify(user, never()).sendMessage("commands.island.ban.player-banned", TextVariables.NAME,
                mockPlayer.getName(), TextVariables.DISPLAY_NAME, mockPlayer.getDisplayName());
        verify(mockPlayer, never()).sendMessage("commands.island.ban.owner-banned-you");
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

        String[] names = { "adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe" };
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

        when(island.isBanned(any(UUID.class)))
                .thenAnswer((Answer<Boolean>) invocation -> banned.contains(invocation.getArgument(0, UUID.class)));
        // Create the names
        when(pm.getName(any(UUID.class))).then((Answer<String>) invocation -> online
                .getOrDefault(invocation.getArgument(0, UUID.class), "tastybento"));

        // Return a set of online players
        mockedBukkit.when(() -> Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

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
        assertFalse(result.isPresent());

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("d");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().toList();
        // Compare the expected with the actual
        String[] expectedName = { "dave" };
        assertTrue(Arrays.equals(expectedName, r.toArray()));

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("fr");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        r = result.get().stream().sorted().toList();
        // Compare the expected with the actual
        String[] expected = { "frank", "freddy" };
        assertTrue(Arrays.equals(expected, r.toArray()));
    }
}
