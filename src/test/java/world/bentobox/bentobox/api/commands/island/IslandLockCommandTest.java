package world.bentobox.bentobox.api.commands.island;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Tests for {@link IslandLockCommand}.
 *
 * @author tastybento
 */
class IslandLockCommandTest extends RanksManagerTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private LocalesManager testLm;

    private IslandLockCommand ilc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        when(user.getLocation()).thenReturn(mock(Location.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");

        // Island
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Locales
        when(testLm.get(any(User.class), anyString())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(testLm);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));

        // Default: island is unlocked (getFlag returns VISITOR_RANK = 0)
        when(island.getFlag(any(Flag.class))).thenReturn(RanksManager.VISITOR_RANK);

        // No players on island by default
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

        // Class
        ilc = new IslandLockCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for constructor.
     */
    @Test
    void testIslandLockCommand() {
        assertEquals("lock", ilc.getLabel());
    }

    /**
     * Test method for {@link IslandLockCommand#setup()}.
     */
    @Test
    void testSetup() {
        assertTrue(ilc.isOnlyPlayer());
        assertEquals("bskyblock.island.lock", ilc.getPermission());
        assertEquals("commands.island.lock.description", ilc.getDescription());
        assertTrue(ilc.isConfigurableRankCommand());
    }

    /**
     * Test canExecute when player has no island and is not in a team.
     */
    @Test
    void testCanExecuteNoIsland() {
        assertFalse(ilc.canExecute(user, "lock", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test canExecute when player has insufficient rank.
     */
    @Test
    void testCanExecuteLowRank() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(ilc.canExecute(user, "lock", Collections.emptyList()));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test canExecute when player has island and sufficient rank.
     */
    @Test
    void testCanExecuteHasIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        assertTrue(ilc.canExecute(user, "lock", Collections.emptyList()));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test canExecute when player is in a team (but has no own island).
     */
    @Test
    void testCanExecuteInTeam() {
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        assertTrue(ilc.canExecute(user, "lock", Collections.emptyList()));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test execute when island is unlocked - should lock it.
     */
    @Test
    void testExecuteLockIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // Island is currently unlocked (VISITOR_RANK = 0)
        when(island.getFlag(eq(Flags.LOCK))).thenReturn(RanksManager.VISITOR_RANK);

        assertTrue(ilc.execute(user, "lock", Collections.emptyList()));

        verify(island).setFlag(eq(Flags.LOCK), eq(RanksManager.MEMBER_RANK));
        verify(user).sendMessage("commands.island.lock.locked");
    }

    /**
     * Test execute when island is locked - should unlock it.
     */
    @Test
    void testExecuteUnlockIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // Island is currently locked (MEMBER_RANK = 500)
        when(island.getFlag(eq(Flags.LOCK))).thenReturn(RanksManager.MEMBER_RANK);

        assertTrue(ilc.execute(user, "lock", Collections.emptyList()));

        verify(island).setFlag(eq(Flags.LOCK), eq(RanksManager.VISITOR_RANK));
        verify(user).sendMessage("commands.island.lock.unlocked");
    }

    /**
     * Test execute when locking with visitors on island - visitors should be expelled.
     */
    @Test
    void testExecuteLockWithVisitorsOnIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getFlag(eq(Flags.LOCK))).thenReturn(RanksManager.VISITOR_RANK);

        // Set up a visitor on the island
        UUID visitorUUID = UUID.randomUUID();
        Player visitorPlayer = mock(Player.class);
        when(visitorPlayer.getUniqueId()).thenReturn(visitorUUID);
        when(visitorPlayer.getLocation()).thenReturn(mock(Location.class));
        when(visitorPlayer.getWorld()).thenReturn(world);
        when(visitorPlayer.spigot()).thenReturn(spigot);
        User.getInstance(visitorPlayer);

        // Visitor is on the island but NOT a team member
        when(island.onIsland(any(Location.class))).thenReturn(true);
        when(island.inTeam(eq(visitorUUID))).thenReturn(false);
        when(island.getPlayersOnIsland()).thenReturn(List.of(visitorPlayer));
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(visitorPlayer));

        // Visitor has an island to go home to
        when(im.hasIsland(any(), eq(visitorUUID))).thenReturn(true);

        assertTrue(ilc.execute(user, "lock", Collections.emptyList()));

        verify(island).setFlag(eq(Flags.LOCK), eq(RanksManager.MEMBER_RANK));
        verify(user).sendMessage("commands.island.lock.locked");
        // Visitor is sent home
        verify(im).homeTeleportAsync(any(), eq(visitorPlayer));
    }

    /**
     * Test execute when locking with visitors on island who have no home - visitors sent to spawn.
     */
    @Test
    void testExecuteLockWithVisitorsNoHome() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getFlag(eq(Flags.LOCK))).thenReturn(RanksManager.VISITOR_RANK);

        // Set up a visitor
        UUID visitorUUID = UUID.randomUUID();
        Player visitorPlayer = mock(Player.class);
        when(visitorPlayer.getUniqueId()).thenReturn(visitorUUID);
        when(visitorPlayer.getLocation()).thenReturn(mock(Location.class));
        when(visitorPlayer.getWorld()).thenReturn(world);
        when(visitorPlayer.spigot()).thenReturn(spigot);
        User.getInstance(visitorPlayer);

        when(island.onIsland(any(Location.class))).thenReturn(true);
        when(island.inTeam(eq(visitorUUID))).thenReturn(false);
        when(island.getPlayersOnIsland()).thenReturn(List.of(visitorPlayer));
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(visitorPlayer));

        // Visitor has no island; spawn is available
        when(im.hasIsland(any(), eq(visitorUUID))).thenReturn(false);
        when(im.inTeam(any(), eq(visitorUUID))).thenReturn(false);
        when(im.getSpawn(any())).thenReturn(Optional.of(island));

        assertTrue(ilc.execute(user, "lock", Collections.emptyList()));

        verify(island).setFlag(eq(Flags.LOCK), eq(RanksManager.MEMBER_RANK));
        verify(user).sendMessage("commands.island.lock.locked");
        // Visitor is sent to spawn
        verify(im).spawnTeleport(any(), eq(visitorPlayer));
    }

    /**
     * Test execute when locking - team members on island are NOT expelled.
     */
    @Test
    void testExecuteLockMembersNotExpelled() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getFlag(eq(Flags.LOCK))).thenReturn(RanksManager.VISITOR_RANK);

        // Set up a team member on the island
        UUID memberUUID = UUID.randomUUID();
        Player memberPlayer = mock(Player.class);
        when(memberPlayer.getUniqueId()).thenReturn(memberUUID);
        when(memberPlayer.getLocation()).thenReturn(mock(Location.class));
        when(memberPlayer.getWorld()).thenReturn(world);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(memberPlayer));

        when(island.onIsland(any(Location.class))).thenReturn(true);
        // This player IS a team member
        when(island.inTeam(eq(memberUUID))).thenReturn(true);
        when(island.getPlayersOnIsland()).thenReturn(List.of(memberPlayer));

        assertTrue(ilc.execute(user, "lock", Collections.emptyList()));

        verify(island).setFlag(eq(Flags.LOCK), eq(RanksManager.MEMBER_RANK));
        verify(user).sendMessage("commands.island.lock.locked");
        // Team member is NOT teleported away
        verify(im, never()).homeTeleportAsync(any(), eq(memberPlayer));
        verify(im, never()).spawnTeleport(any(), eq(memberPlayer));
    }

    /**
     * Test execute when island is null (safety check).
     */
    @Test
    void testExecuteNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);

        assertFalse(ilc.execute(user, "lock", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }
}
