package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 */
public class IslandTeamKickCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Player target;
    @Mock
    private CompositeCommand subCommand;
    @Mock
    private Addon addon;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

        // Player
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(notUUID);
        when(target.isOnline()).thenReturn(true);
        when(target.getName()).thenReturn("poslovitch");
        when(target.getDisplayName()).thenReturn("&Cposlovich");
        when(target.spigot()).thenReturn(spigot);
        when(target.getWorld()).thenReturn(world);
        // Set the target user
        User.getInstance(target);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getDisplayName()).thenReturn("&Ctastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        subCommand = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalCommand = Optional.of(subCommand);
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(optionalCommand);
        when(ic.getAddon()).thenReturn(addon);
        AddonDescription desc = new AddonDescription.Builder("main", "name", "version").build();
        when(addon.getDescription()).thenReturn(desc);
        when(ic.getWorld()).thenReturn(world);

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // when(im.isOwner(any(), any())).thenReturn(true);
        // when(im.getOwner(any(), any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Island
        when(island.getUniqueId()).thenReturn("uniqueid");
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.VISITOR_RANK);

        // Ranks
        when(island.getRank(uuid)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRank(notUUID)).thenReturn(RanksManager.MEMBER_RANK);
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        verify(user).sendMessage(eq("general.errors.no-team"));
    }

    /**
     * Test method for {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteLowerTeamRank() {
        when(island.getRank(user)).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRank(notUUID)).thenReturn(RanksManager.SUB_OWNER_RANK);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));
        when(island.inTeam(notUUID)).thenReturn(true);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage(eq("commands.island.team.kick.cannot-kick-rank"), eq(TextVariables.NAME), eq("poslovitch"));
    }

    /**
     * Test method for {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteEqualTeamRank() {
        when(island.getRank(user)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRank(notUUID)).thenReturn(RanksManager.SUB_OWNER_RANK);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));
        when(island.inTeam(notUUID)).thenReturn(true);
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage(eq("commands.island.team.kick.cannot-kick-rank"), eq(TextVariables.NAME), eq("poslovitch"));
    }

    /**
     * Test method for {@link IslandTeamKickCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteLargerTeamRank() {
        when(island.getRank(user)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRank(notUUID)).thenReturn(RanksManager.MEMBER_RANK);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(im).removePlayer(any(Island.class), eq(notUUID));
        verify(user).sendMessage("commands.island.team.kick.success", TextVariables.NAME, "poslovitch", TextVariables.DISPLAY_NAME, "&Cposlovich");
    }

    /**
     * Test method for {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoCommandRank() {
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRank(user)).thenReturn(RanksManager.MEMBER_RANK);

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for
     * {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show help
    }

    /**
     * Test method for
     * {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "poslovitch");
    }

    /**
     * Test method for
     * {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteSamePlayer() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        when(pm.getUUID(any())).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage(eq("commands.island.team.kick.cannot-kick"));
    }

    /**
     * Test method for
     * {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteDifferentPlayerNotInTeam() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        // when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage(eq("general.errors.not-in-team"));
    }

    /**
     * Test method for
     * {@link IslandTeamKickCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteDifferentPlayerNoRank() {
        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for {@link IslandTeamKickCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isKickConfirmation()).thenReturn(false);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(im).removePlayer(any(Island.class), eq(notUUID));
        verify(user).sendMessage("commands.island.team.kick.success", TextVariables.NAME, "poslovitch", TextVariables.DISPLAY_NAME, "&Cposlovich");
    }

    /**
     * Test method for {@link IslandTeamKickCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoConfirmationKeepInventory() {
        when(iwm.isOnLeaveResetInventory(any())).thenReturn(true);
        when(iwm.isKickedKeepInventory(any())).thenReturn(true);
        when(s.isKickConfirmation()).thenReturn(false);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(im).removePlayer(any(Island.class), eq(notUUID));
        verify(user).sendMessage("commands.island.team.kick.success", TextVariables.NAME, "poslovitch", TextVariables.DISPLAY_NAME, "&Cposlovich");
        verify(target, never()).getInventory();

    }

    /**
     * Test method for {@link IslandTeamKickCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoConfirmationLoseInventoryOffline() {
        when(iwm.isOnLeaveResetInventory(any())).thenReturn(true);
        when(iwm.isKickedKeepInventory(any())).thenReturn(false);
        when(s.isKickConfirmation()).thenReturn(false);

        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(notUUID)).thenReturn("poslovitch");
        Players targetPlayer = mock(Players.class);
        when(pm.getPlayer(eq(notUUID))).thenReturn(targetPlayer);

        when(target.isOnline()).thenReturn(false);

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        verify(im).removePlayer(any(Island.class), eq(notUUID));
        verify(user).sendMessage("commands.island.team.kick.success", TextVariables.NAME, "poslovitch", TextVariables.DISPLAY_NAME, "&Cposlovich");
        verify(target, Mockito.never()).getInventory();
        verify(pm).cleanLeavingPlayer(any(), any(User.class), eq(true), eq(island));
    }

    /**
     * Test method for {@link IslandTeamKickCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteWithConfirmation() {
        when(s.isKickConfirmation()).thenReturn(true);

        when(pm.getUUID(any())).thenReturn(notUUID);

        when(island.getMemberSet()).thenReturn(ImmutableSet.of(notUUID));

        IslandTeamKickCommand itl = new IslandTeamKickCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("poslovitch")));
        // Confirmation required
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("0"));
    }

    /**
     * Test method for {@link IslandTeamKickCommand#setCooldown(UUID, UUID, int)}
     */
    @Test
    public void testCooldown() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        testExecuteNoConfirmation();
        verify(subCommand).setCooldown("uniqueid", notUUID.toString(), 600);
    }

    @Test
    public void testTabCompleteNoArgument() {

        Builder<UUID> memberSet = new ImmutableSet.Builder<>();
        for (int j = 0; j < 11; j++) {
            memberSet.add(UUID.randomUUID());
        }

        when(island.getMemberSet()).thenReturn(memberSet.build());
        // Return a set of players
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george",
                "harry", "ian", "joe");
        when(island.getRank(any(UUID.class))).thenReturn(RanksManager.COOP_RANK, RanksManager.COOP_RANK,
                RanksManager.COOP_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK);

        IslandTeamKickCommand ibc = new IslandTeamKickCommand(ic);
        // Get the tab-complete list with no argument
        Optional<List<String>> result = ibc.tabComplete(user, "", new LinkedList<>());
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().toList();
        // Compare the expected with the actual - first names in the list
        String[] expectedNames = { "adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george" };
        int i = 0;
        for (String name : r) {
            assertEquals(expectedNames[i++], name, "Rank " + i);
        }
    }

    @Test
    public void testTabCompleteWithArgument() {

        Builder<UUID> memberSet = new ImmutableSet.Builder<>();
        for (int j = 0; j < 11; j++) {
            memberSet.add(UUID.randomUUID());
        }

        when(island.getMemberSet()).thenReturn(memberSet.build());
        // Return a set of players
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george",
                "harry", "ian", "joe");
        when(island.getRank(any(UUID.class))).thenReturn(RanksManager.COOP_RANK, RanksManager.COOP_RANK,
                RanksManager.COOP_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK);

        IslandTeamKickCommand ibc = new IslandTeamKickCommand(ic);
        // Get the tab-complete list with argument
        Optional<List<String>> result = ibc.tabComplete(user, "", Collections.singletonList("g"));
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().toList();
        assertFalse(r.isEmpty());
        // Compare the expected with the actual
        String[] expectedNames = { "george" };
        int i = 0;
        for (String name : r) {
            assertEquals(expectedNames[i++], name, "Rank " + i);
        }

        // assertTrue(Arrays.equals(expectedNames, r.toArray()));

    }

    @Test
    public void testTabCompleteWithWrongArgument() {

        Builder<UUID> memberSet = new ImmutableSet.Builder<>();
        for (int j = 0; j < 11; j++) {
            memberSet.add(UUID.randomUUID());
        }

        when(island.getMemberSet()).thenReturn(memberSet.build());
        // Return a set of players
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george",
                "harry", "ian", "joe");
        when(island.getRank(any(User.class))).thenReturn(RanksManager.COOP_RANK, RanksManager.COOP_RANK,
                RanksManager.COOP_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK, RanksManager.MEMBER_RANK,
                RanksManager.MEMBER_RANK);

        IslandTeamKickCommand ibc = new IslandTeamKickCommand(ic);
        // Get the tab-complete list with argument
        LinkedList<String> args = new LinkedList<>();
        args.add("c");
        Optional<List<String>> result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());

    }
}
