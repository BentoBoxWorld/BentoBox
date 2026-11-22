package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import net.kyori.adventure.text.Component;

class LockAndBanListenerTest extends CommonTestSetup {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    @Mock
    private User user;
    // Class under test
    private LockAndBanListener listener;
    @Mock
    private Location outside;
    @Mock
    private Location inside;
    @Mock
    private Location inside2;
    private Settings s;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Island world manager
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Settings
        s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Sometimes use withSettings().verboseLogging()
        User.setPlugin(plugin);
        // User and player are not op
        when(user.isOp()).thenReturn(false);
        when(mockPlayer.isOp()).thenReturn(false);
        when(mockPlayer.getWorld()).thenReturn(world);
        // No special perms
        when(mockPlayer.hasPermission(anyString())).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(X);
        when(loc.getBlockY()).thenReturn(Y);
        when(loc.getBlockZ()).thenReturn(Z);
        when(island.getCenter()).thenReturn(loc);
        when(loc.clone()).thenReturn(loc);
        when(island.getProtectionRange()).thenReturn(PROTECTION_RANGE);
        // Island is not locked by default
        when(island.isAllowed(any(), any())).thenReturn(true);

        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // Create the listener object
        listener = new LockAndBanListener();

        // Common from to's
        when(outside.getWorld()).thenReturn(world);
        when(outside.getBlockX()).thenReturn(X + PROTECTION_RANGE + 1);
        when(outside.getBlockY()).thenReturn(Y);
        when(outside.getBlockZ()).thenReturn(Z);
        when(outside.clone()).thenReturn(outside);

        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);
        when(inside.clone()).thenReturn(inside);

        when(inside2.getWorld()).thenReturn(world);
        when(inside2.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
        when(inside2.getBlockY()).thenReturn(Y);
        when(inside2.getBlockZ()).thenReturn(Z);
        when(inside2.clone()).thenReturn(inside2);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(inside)).thenReturn(opIsland);
        when(im.getProtectedIslandAt(inside2)).thenReturn(opIsland);
        when(im.getProtectedIslandAt(outside)).thenReturn(Optional.empty());

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        when(iwm.getFriendlyName(world)).thenReturn("BSkyblock");

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testTeleportToNotBannedIsland() {
        // Setup location outside island, one inside banned island
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Simulate a teleport into an island
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should not be cancelled
        assertFalse(e.isCancelled());
        // User should see no message from this class
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void testTeleportToBannedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // Simulate a teleport into an island
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, outside, inside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should be cancelled
        assertTrue(e.isCancelled());
    }

    
    @Test
    void testLoginToBannedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(mockPlayer.getLocation()).thenReturn(inside);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(mockPlayer, Component.text("join message")));
        // User should be teleported somewhere
        verify(im).homeTeleportAsync(any(), eq(mockPlayer));
        // Call teleport event
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should not be cancelled
        assertFalse(e.isCancelled());
    }

    @Test
    void testVerticalMoveOnly() {
        // Move vertically only
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.getBlockX()).thenReturn(X);
        when(from.getBlockY()).thenReturn(50);
        when(from.getBlockZ()).thenReturn(Z);
        Location to = mock(Location.class);
        when(to.getWorld()).thenReturn(world);
        when(to.getBlockX()).thenReturn(X);
        when(to.getBlockY()).thenReturn(55);
        when(to.getBlockZ()).thenReturn(Z);
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), from, to);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
        // Confirm no check is done on the island
        verify(im, never()).getProtectedIslandAt(any());
    }

    @Test
    void testVerticalVehicleMoveOnly() {
        // Move vertically only
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.getBlockX()).thenReturn(X);
        when(from.getBlockY()).thenReturn(50);
        when(from.getBlockZ()).thenReturn(Z);
        when(from.clone()).thenReturn(from);
        Location to = mock(Location.class);
        when(to.getWorld()).thenReturn(world);
        when(to.getBlockX()).thenReturn(X);
        when(to.getBlockY()).thenReturn(55);
        when(to.getBlockZ()).thenReturn(Z);
        when(to.clone()).thenReturn(to);
        // Create vehicle and put two players in it.
        Vehicle vehicle = mock(Vehicle.class);
        Player player2 = mock(Player.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(user.getPlayer());
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        // Move vehicle
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, from, to));
        // Confirm no check is done on the island
        verify(im, never()).getProtectedIslandAt(any());
    }

    @Test
    void testVehicleMoveNoPlayerPassengersSkipsIslandLookup() {
        // A vehicle carrying only non-player entities (e.g. a mob-ridden horse or an
        // empty minecart) must not trigger an island lookup when it crosses a boundary.
        Vehicle vehicle = mock(Vehicle.class);
        Entity mob = mock(Entity.class); // not a Player
        List<Entity> passengers = new ArrayList<>();
        passengers.add(mob);
        when(vehicle.getPassengers()).thenReturn(passengers);
        // Horizontal move across a block boundary
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // Lazy lookup: no player passenger, so the grid is never queried
        verify(im, never()).getProtectedIslandAt(any());
    }

    @Test
    void testVehicleMoveResolvesIslandOnceForMultiplePlayers() {
        // Two players in one vehicle should share a single island lookup, not one each.
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        Vehicle vehicle = mock(Vehicle.class);
        Player player2 = mock(Player.class);
        when(player2.isOp()).thenReturn(false);
        when(player2.hasPermission(anyString())).thenReturn(false);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(mockPlayer);
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        // Horizontal move across a block boundary into an open island
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // Resolved lazily on the first player and reused for the second
        verify(im, times(1)).getProtectedIslandAt(inside);
    }

    @Test
    void testPlayerMoveIntoBannedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // User should NOT be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(mockPlayer));
    }

    @Test
    void testPlayerMoveInsideBannedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(mockPlayer.getLocation()).thenReturn(inside);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, inside, inside2);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // User should be teleported somewhere
        verify(sch).runTask(any(), any(Runnable.class));
        // Call teleport event
        PlayerTeleportEvent ev = new PlayerTeleportEvent(mockPlayer, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(ev);
        // Should not be cancelled
        assertFalse(ev.isCancelled());
    }

    @Test
    void testVehicleMoveIntoBannedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // Add the user to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // Create vehicle and put two players in it. One is banned, the other is not
        Vehicle vehicle = mock(Vehicle.class);
        Player player2 = mock(Player.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(mockPlayer);
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        when(vehicle.getWorld()).thenReturn(world);
        // Move vehicle
        when(vehicle.teleport(outside)).thenReturn(true);
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // The whole vehicle is turned back with both riders aboard
        verify(vehicle).teleport(outside);
        verify(mockPlayer, never()).leaveVehicle();
        // Nobody is sent home
        verify(im, never()).homeTeleportAsync(any(), eq(mockPlayer));
        verify(im, never()).homeTeleportAsync(any(), eq(player2));
        // Call teleport event
        PlayerTeleportEvent ev = new PlayerTeleportEvent(mockPlayer, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(ev);
        // Should not be cancelled
        assertFalse(ev.isCancelled());
    }

    /*
     * Island lock tests
     */

    @Test
    void testTeleportToLockedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);
        // Simulate a teleport into an island
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, outside, inside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should be cancelled
        assertTrue(e.isCancelled());
    }


    @Test
    void testLoginToLockedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(mockPlayer.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(mockPlayer, Component.text("join message")));
        // User should be teleported somewhere
        verify(im).homeTeleportAsync(any(), eq(mockPlayer));
        // Call teleport event
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should not be cancelled
        assertFalse(e.isCancelled());
    }

    @Test
    void testLoginToLockedIslandAsOp() {
        // Make player
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(true);

        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, Component.text("join message")));
        // User should not see a message
        verify(notifier, never()).notify(any(), anyString());
        // User should not be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(player));
    }

    @Test
    void testLoginToLockedIslandWithBypassPerm() {
        // Make player
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getWorld()).thenReturn(world);

        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, Component.text("join message")));
        // User should see a bypass message
        verify(notifier).notify(any(), anyString());
        // User should not be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(player));
    }

    @Test
    void testLoginToLockedIslandAsMember() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, Component.text("join message")));
        // User should not see a message
        verify(notifier, never()).notify(any(), anyString());
        // User should not be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(player));
    }

    @Test
    void testPlayerMoveIntoLockedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // User should NOT be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(mockPlayer));
    }

    @Test
    void testPlayerMoveIntoLockedIslandAsOp() {
        // Make player
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(true);

        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);

        // Lock island for player
        doReturn(false).when(island).isAllowed(any(User.class), eq(Flags.LOCK));
        
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testPlayerMoveIntoLockedIslandAsNPC() {
        // Make player
        Player player = mock(Player.class);
        when(player.hasMetadata("NPC")).thenReturn(true);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testPlayerMoveIntoLockedIslandWithBypass() {
        // Make player
        when(mockPlayer.isOp()).thenReturn(false);
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);

        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
        // User should see a bypass notification
        verify(notifier).notify(any(), anyString());
    }

    @Test
    void testPlayerMoveIntoLockedIslandAsMember() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        // Should not be cancelled
        assertFalse(e.isCancelled());
        // Player should not see a message
        verify(notifier, never()).notify(any(), anyString());
        // User should NOT be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(player));
    }

    @Test
    void testPlayerMoveInsideLockedIsland() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(mockPlayer.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, inside, inside2);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // User should be teleported somewhere
        verify(sch).runTask(any(), any(Runnable.class));
        // Call teleport event
        PlayerTeleportEvent ev = new PlayerTeleportEvent(mockPlayer, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(ev);
        // Should not be cancelled
        assertFalse(ev.isCancelled());
    }

    @Test
    void testPlayerMoveInsideLockedIslandAsOp() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOp()).thenReturn(true);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testPlayerMoveInsideLockedIslandWithBypass() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
        // User should see a bypass notification
        verify(notifier).notify(any(), anyString());
    }

    @Test
    void testPlayerMoveInsideLockedIslandAsMember() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
        // Player should not see a message
        verify(notifier, never()).notify(any(), anyString());
        // User should not be teleported somewhere
        verify(im, never()).homeTeleportAsync(any(), eq(player));
    }

    /**
     * A vehicle whose only rider is locked out is turned back with the rider still
     * aboard, so no boat is left stranded on the locked island (#2958).
     */
    @Test
    void testVehicleWithOnlyLockedOutRiderIsTurnedBack() {
        Vehicle vehicle = lockedOutRiderIn(mockPlayer);
        when(vehicle.teleport(outside)).thenReturn(true);

        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));

        verify(vehicle).teleport(outside);
        verify(mockPlayer, never()).leaveVehicle();
        verify(im, never()).homeTeleportAsync(any(), eq(mockPlayer));
    }

    /**
     * If the server will not move the loaded vehicle, the rider is put back where
     * they came from and the empty vehicle is brought out too, so it is recoverable.
     */
    @Test
    void testVehicleTurnBackFallsBackToMovingRiderAndVehicleSeparately() {
        Vehicle vehicle = lockedOutRiderIn(mockPlayer);
        when(vehicle.teleport(outside)).thenReturn(false);

        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));

        verify(mockPlayer).leaveVehicle();
        verify(mockPlayer).teleport(outside);
        verify(vehicle, times(2)).teleport(outside);
        verify(im, never()).homeTeleportAsync(any(), eq(mockPlayer));
    }

    /**
     * A rider who is already inside an island they may not be on (it was locked
     * around them) is sent home rather than shuffled within the island.
     */
    @Test
    void testVehicleRiderAlreadyInsideLockedIslandIsSentHome() {
        Vehicle vehicle = lockedOutRiderIn(mockPlayer);

        listener.onVehicleMove(new VehicleMoveEvent(vehicle, inside, inside2));

        verify(mockPlayer).leaveVehicle();
        verify(im).homeTeleportAsync(any(), eq(mockPlayer));
        verify(vehicle, never()).teleport(any(Location.class));
    }

    /**
     * If the server will not move a vehicle carrying both a locked-out rider and a
     * member, everyone is taken off and put back where the vehicle came from.
     */
    @Test
    void testMixedVehicleFallbackMovesEveryRiderBack() {
        Vehicle vehicle = lockedOutRiderIn(mockPlayer);
        Player member = mock(Player.class);
        UUID memberUuid = UUID.randomUUID();
        when(member.getUniqueId()).thenReturn(memberUuid);
        when(member.getWorld()).thenReturn(world);
        vehicle.getPassengers().add(member);
        when(island.isAllowed(any(User.class), any())).thenAnswer(
                (Answer<Boolean>) invocation -> invocation.getArgument(0, User.class).getUniqueId().equals(memberUuid));
        when(vehicle.teleport(outside)).thenReturn(false);

        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));

        verify(mockPlayer).teleport(outside);
        verify(member).teleport(outside);
        verify(vehicle, times(2)).teleport(outside);
        verify(im, never()).homeTeleportAsync(any(), any(Player.class));
    }

    /*
     * Locked island border tests
     */

    private WorldBorder borderSetup(Settings.BorderColor color) {
        when(s.isLockedBorder()).thenReturn(true);
        when(s.getLockedBorderColor()).thenReturn(color);
        when(island.getUniqueId()).thenReturn("island-1");
        when(island.getProtectionCenter()).thenReturn(new Location(world, X, Y, Z));
        WorldBorder border = mock(WorldBorder.class);
        mockedBukkit.when(Bukkit::createWorldBorder).thenReturn(border);
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);
        return border;
    }

    @Test
    void testLockedIslandBorderShownWhenTurnedAway() {
        WorldBorder border = borderSetup(Settings.BorderColor.RED);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(border).setSize(PROTECTION_RANGE * 2D);
        // Red is a shrinking border
        verify(border).changeSize(eq(PROTECTION_RANGE * 2D - 0.1), anyLong());
        verify(mockPlayer).setWorldBorder(border);
        verify(sch).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    @Test
    void testLockedIslandBorderBlueIsStatic() {
        WorldBorder border = borderSetup(Settings.BorderColor.BLUE);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(border, never()).changeSize(anyDouble(), anyLong());
        verify(mockPlayer).setWorldBorder(border);
    }

    @Test
    void testLockedIslandBorderGreenGrows() {
        WorldBorder border = borderSetup(Settings.BorderColor.GREEN);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(border).changeSize(eq(PROTECTION_RANGE * 2D + 0.1), anyLong());
    }

    @Test
    void testLockedIslandBorderNotResentWhileShowing() {
        WorldBorder border = borderSetup(Settings.BorderColor.RED);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));
        when(mockPlayer.getWorldBorder()).thenReturn(border);
        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(mockPlayer, times(1)).setWorldBorder(border);
    }

    @Test
    void testLockedIslandBorderOffInConfig() {
        borderSetup(Settings.BorderColor.RED);
        when(s.isLockedBorder()).thenReturn(false);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(mockPlayer, never()).setWorldBorder(any());
    }

    @Test
    void testLockedIslandBorderNotShownForBan() {
        borderSetup(Settings.BorderColor.RED);
        when(island.isBanned(uuid)).thenReturn(true);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));

        verify(mockPlayer, never()).setWorldBorder(any());
    }

    @Test
    void testLockedIslandBorderKeptUntilDue() {
        WorldBorder border = borderSetup(Settings.BorderColor.RED);
        WorldBorder previous = mock(WorldBorder.class);
        when(mockPlayer.getWorldBorder()).thenReturn(previous);

        listener.onPlayerMove(new PlayerMoveEvent(mockPlayer, outside, inside));
        when(mockPlayer.getWorldBorder()).thenReturn(border);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskLater(any(), task.capture(), anyLong());
        // Not yet due: the task waits again rather than removing the border
        task.getValue().run();
        verify(mockPlayer, never()).setWorldBorder(previous);
        verify(sch, times(2)).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    private Vehicle lockedOutRiderIn(Player rider) {
        when(rider.getUniqueId()).thenReturn(uuid);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);
        Vehicle vehicle = mock(Vehicle.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(rider);
        when(vehicle.getPassengers()).thenReturn(passengers);
        when(vehicle.getWorld()).thenReturn(world);
        return vehicle;
    }

    @Test
    void testVehicleMoveIntoLockedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        Player player2 = mock(Player.class);
        UUID uuid2 = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player2.getWorld()).thenReturn(world);

        // Player 1 is not a member, player 2 is an island member
        when(island.isAllowed(any(User.class), any())).thenAnswer(
                (Answer<Boolean>) invocation -> invocation.getArgument(0, User.class).getUniqueId().equals(uuid2));

        // Create vehicle and put two players in it. One is a member, the other is not
        Vehicle vehicle = mock(Vehicle.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(player);
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        when(vehicle.getWorld()).thenReturn(world);
        // Move vehicle
        when(vehicle.teleport(outside)).thenReturn(true);
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // One rider may not enter, so the whole vehicle is turned back, member included
        verify(vehicle).teleport(outside);
        verify(player, never()).leaveVehicle();
        verify(player2, never()).leaveVehicle();
        // Nobody is sent home
        verify(im, never()).homeTeleportAsync(any(), eq(player));
        verify(im, never()).homeTeleportAsync(any(), eq(player2));
        // Call teleport event
        PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(ev);
        // Should not be cancelled
        assertFalse(ev.isCancelled());
    }

    /*
     * Notification deduplication tests
     */

    @Test
    void testPlayerMoveIntoLockedIslandNotifiesOnlyOnce() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // First move attempt — should notify
        PlayerMoveEvent e1 = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e1);
        assertTrue(e1.isCancelled());

        // Second move attempt — should NOT notify again
        PlayerMoveEvent e2 = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e2);
        assertTrue(e2.isCancelled());

        // Verify notification was sent only once
        verify(notifier, times(1)).notify(any(), anyString());
    }

    @Test
    void testPlayerMoveIntoBannedIslandNotifiesOnlyOnce() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Add player to the ban list
        when(island.isBanned(uuid)).thenReturn(true);

        // First move attempt — should notify
        PlayerMoveEvent e1 = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e1);
        assertTrue(e1.isCancelled());

        // Second move attempt — should NOT notify again
        PlayerMoveEvent e2 = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e2);
        assertTrue(e2.isCancelled());

        // Verify notification was sent only once
        verify(notifier, times(1)).notify(any(), anyString());
    }

    @Test
    void testPlayerReEntryNotifiesAgain() {
        // Make player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(mockPlayer.getLocation()).thenReturn(outside);

        // Lock island for player
        when(island.isAllowed(any(User.class), eq(Flags.LOCK))).thenReturn(false);

        // First move attempt into locked island — should notify
        PlayerMoveEvent e1 = new PlayerMoveEvent(mockPlayer, outside, inside);
        listener.onPlayerMove(e1);
        assertTrue(e1.isCancelled());

        // Player moves away from the island (to a different open area) — clears notification state
        Location outside2 = mock(Location.class);
        when(outside2.getWorld()).thenReturn(world);
        when(outside2.getBlockX()).thenReturn(X + PROTECTION_RANGE + 50);
        when(outside2.getBlockY()).thenReturn(Y);
        when(outside2.getBlockZ()).thenReturn(Z);
        when(outside2.clone()).thenReturn(outside2);
        when(im.getProtectedIslandAt(outside2)).thenReturn(Optional.empty());

        PlayerMoveEvent e2 = new PlayerMoveEvent(mockPlayer, outside, outside2);
        listener.onPlayerMove(e2);
        assertFalse(e2.isCancelled());

        // Player attempts to enter locked island again — should notify again
        PlayerMoveEvent e3 = new PlayerMoveEvent(mockPlayer, outside2, inside);
        listener.onPlayerMove(e3);
        assertTrue(e3.isCancelled());

        // Verify notification was sent twice (once per entry attempt)
        verify(notifier, times(2)).notify(any(), anyString());
    }

}
