package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class })
public class LockAndBanListenerTest {
  private static final Integer PROTECTION_RANGE = 200;
  private static final Integer X = 600;
  private static final Integer Y = 120;
  private static final Integer Z = 10000;
  private UUID uuid;

  @Mock
  private User user;

  @Mock
  private IslandsManager im;

  @Mock
  private Island island;

  @Mock
  private World world;

  // Class under test
  private LockAndBanListener listener;

  @Mock
  private Location outside;

  @Mock
  private Location inside;

  @Mock
  private Notifier notifier;

  @Mock
  private Location inside2;

  @Mock
  private BukkitScheduler sch;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Set up plugin
    BentoBox plugin = mock(BentoBox.class);
    Whitebox.setInternalState(BentoBox.class, "instance", plugin);

    // Island world manager
    IslandWorldManager iwm = mock(IslandWorldManager.class);
    when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

    when(plugin.getIWM()).thenReturn(iwm);

    // Settings
    Settings s = mock(Settings.class);
    when(plugin.getSettings()).thenReturn(s);

    // Player
    Player player = mock(Player.class);
    // Sometimes use withSettings().verboseLogging()
    User.setPlugin(plugin);
    // User and player are not op
    when(user.isOp()).thenReturn(false);
    when(player.isOp()).thenReturn(false);
    // No special perms
    when(player.hasPermission(anyString())).thenReturn(false);
    uuid = UUID.randomUUID();
    when(user.getUniqueId()).thenReturn(uuid);
    when(user.getPlayer()).thenReturn(player);
    when(user.getName()).thenReturn("tastybento");

    // No island for player to begin with (set it later in the tests)
    when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
    when(im.isOwner(any(), eq(uuid))).thenReturn(false);
    when(plugin.getIslands()).thenReturn(im);

    // Has team
    PlayersManager pm = mock(PlayersManager.class);
    when(im.inTeam(any(), eq(uuid))).thenReturn(true);
    when(plugin.getPlayers()).thenReturn(pm);

    // Server & Scheduler
    PowerMockito.mockStatic(Bukkit.class);
    when(Bukkit.getScheduler()).thenReturn(sch);

    // Locales
    LocalesManager lm = mock(LocalesManager.class);
    when(plugin.getLocalesManager()).thenReturn(lm);
    when(lm.get(any(), any())).thenReturn("mock translation");

    // Placeholders
    PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
    when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
    when(placeholdersManager.replacePlaceholders(any(), any()))
      .thenReturn("mock translation");

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

    when(inside.getWorld()).thenReturn(world);
    when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
    when(inside.getBlockY()).thenReturn(Y);
    when(inside.getBlockZ()).thenReturn(Z);

    when(inside.getWorld()).thenReturn(world);
    when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
    when(inside.getBlockY()).thenReturn(Y);
    when(inside.getBlockZ()).thenReturn(Z);

    Optional<Island> opIsland = Optional.ofNullable(island);
    when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);
    when(im.getProtectedIslandAt(eq(inside2))).thenReturn(opIsland);
    when(im.getProtectedIslandAt(eq(outside))).thenReturn(Optional.empty());

    // Addon
    when(iwm.getAddon(any())).thenReturn(Optional.empty());
  }

  @After
  public void tearDown() {
    User.clearUsers();
    framework().clearInlineMocks();
  }

  @Test
  public void testTeleportToNotBannedIsland() {
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
  public void testTeleportToBannedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);

    // Add player to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);

    // Simulate a teleport into an island
    PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
    // Pass to event listener
    listener.onPlayerTeleport(e);
    // Should be cancelled
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), any());
  }

  @Test
  public void testLoginToBannedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player on the island
    when(player.getLocation()).thenReturn(inside);

    // Add player to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);

    // Log them in
    listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
    // User should see a message
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(im).homeTeleportAsync(any(), eq(player));
    // Call teleport event
    PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(e);
    // Should not be cancelled
    assertFalse(e.isCancelled());
  }

  @Test
  public void testVerticalMoveOnly() {
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
  public void testVerticalVehicleMoveOnly() {
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
  public void testPlayerMoveIntoBannedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player just outside island
    when(player.getLocation()).thenReturn(outside);

    // Add player to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
    listener.onPlayerMove(e);
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), anyString());
    // User should NOT be teleported somewhere
    verify(im, never()).homeTeleportAsync(any(), eq(player));
  }

  @Test
  public void testPlayerMoveInsideBannedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player inside island
    when(player.getLocation()).thenReturn(inside);

    // Add player to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);
    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
    listener.onPlayerMove(e);
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(sch).runTask(any(), any(Runnable.class));
    // Call teleport event
    PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(ev);
    // Should not be cancelled
    assertFalse(ev.isCancelled());
  }

  @Test
  public void testVehicleMoveIntoBannedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);

    // Add player to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);

    // Add the user to the ban list
    when(island.isBanned(eq(uuid))).thenReturn(true);

    // Create vehicle and put two players in it. One is banned, the other is not
    Vehicle vehicle = mock(Vehicle.class);
    Player player2 = mock(Player.class);
    List<Entity> passengers = new ArrayList<>();
    passengers.add(player);
    passengers.add(player2);
    when(vehicle.getPassengers()).thenReturn(passengers);
    when(vehicle.getWorld()).thenReturn(world);
    // Move vehicle
    listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
    // Player should see a message and nothing should be sent to Player 2
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(im).homeTeleportAsync(any(), eq(player));
    // Player 2 should not be teleported
    verify(im, never()).homeTeleportAsync(any(), eq(player2));
    // Call teleport event
    PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(ev);
    // Should not be cancelled
    assertFalse(ev.isCancelled());
  }

  /*
   * Island lock tests
   */

  @Test
  public void testTeleportToLockedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);
    // Simulate a teleport into an island
    PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
    // Pass to event listener
    listener.onPlayerTeleport(e);
    // Should be cancelled
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), any());
  }

  @Test
  public void testTeleportToLockedIslandAsMember() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Simulate a teleport into an island
    PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
    // Pass to event listener
    listener.onPlayerTeleport(e);
    // Should not be not cancelled
    assertFalse(e.isCancelled());
    // Player should not see a message
    verify(notifier, never()).notify(any(), any());
  }

  @Test
  public void testLoginToLockedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player on the island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Log them in
    listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
    // User should see a message
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(im).homeTeleportAsync(any(), eq(player));
    // Call teleport event
    PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(e);
    // Should not be cancelled
    assertFalse(e.isCancelled());
  }

  @Test
  public void testLoginToLockedIslandAsOp() {
    // Make player
    Player player = mock(Player.class);
    when(player.isOp()).thenReturn(true);

    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player on the island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Log them in
    listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
    // User should not see a message
    verify(notifier, never()).notify(any(), anyString());
    // User should not be teleported somewhere
    verify(im, never()).homeTeleportAsync(any(), eq(player));
  }

  @Test
  public void testLoginToLockedIslandWithBypassPerm() {
    // Make player
    Player player = mock(Player.class);
    when(player.isOp()).thenReturn(false);
    when(player.hasPermission(anyString())).thenReturn(true);

    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player on the island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Log them in
    listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
    // User should not see a message
    verify(notifier, never()).notify(any(), anyString());
    // User should not be teleported somewhere
    verify(im, never()).homeTeleportAsync(any(), eq(player));
  }

  @Test
  public void testLoginToLockedIslandAsMember() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player on the island
    when(player.getLocation()).thenReturn(inside);
    // Log them in
    listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
    // User should not see a message
    verify(notifier, never()).notify(any(), anyString());
    // User should not be teleported somewhere
    verify(im, never()).homeTeleportAsync(any(), eq(player));
  }

  @Test
  public void testPlayerMoveIntoLockedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player just outside island
    when(player.getLocation()).thenReturn(outside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
    listener.onPlayerMove(e);
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), anyString());
    // User should NOT be teleported somewhere
    verify(im, never()).homeTeleportAsync(any(), eq(player));
  }

  @Test
  public void testPlayerMoveIntoLockedIslandAsOp() {
    // Make player
    Player player = mock(Player.class);
    when(player.isOp()).thenReturn(true);

    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player just outside island
    when(player.getLocation()).thenReturn(outside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
    listener.onPlayerMove(e);
    assertFalse(e.isCancelled());
  }

  @Test
  public void testPlayerMoveIntoLockedIslandAsNPC() {
    // Make player
    Player player = mock(Player.class);
    when(player.hasMetadata("NPC")).thenReturn(true);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player just outside island
    when(player.getLocation()).thenReturn(outside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
    listener.onPlayerMove(e);
    assertFalse(e.isCancelled());
  }

  @Test
  public void testPlayerMoveIntoLockedIslandWithBypass() {
    // Make player
    Player player = mock(Player.class);
    when(player.isOp()).thenReturn(false);
    when(player.hasPermission(anyString())).thenReturn(true);

    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player just outside island
    when(player.getLocation()).thenReturn(outside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
    listener.onPlayerMove(e);
    assertFalse(e.isCancelled());
  }

  @Test
  public void testPlayerMoveIntoLockedIslandAsMember() {
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
  public void testPlayerMoveInsideLockedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player inside island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
    listener.onPlayerMove(e);
    assertTrue(e.isCancelled());
    // Player should see a message
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(sch).runTask(any(), any(Runnable.class));
    // Call teleport event
    PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(ev);
    // Should not be cancelled
    assertFalse(ev.isCancelled());
  }

  @Test
  public void testPlayerMoveInsideLockedIslandAsOp() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.isOp()).thenReturn(true);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player inside island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
    listener.onPlayerMove(e);
    assertFalse(e.isCancelled());
  }

  @Test
  public void testPlayerMoveInsideLockedIslandWithBypass() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.isOp()).thenReturn(false);
    when(player.hasPermission(anyString())).thenReturn(true);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    // Place the player inside island
    when(player.getLocation()).thenReturn(inside);

    // Lock island for player
    when(island.isAllowed(any(), eq(Flags.LOCK))).thenReturn(false);

    // Move player
    PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
    listener.onPlayerMove(e);
    assertFalse(e.isCancelled());
  }

  @Test
  public void testPlayerMoveInsideLockedIslandAsMember() {
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

  @Test
  public void testVehicleMoveIntoLockedIsland() {
    // Make player
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    // Give player an island
    when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
    Player player2 = mock(Player.class);
    UUID uuid2 = UUID.randomUUID();
    when(player2.getUniqueId()).thenReturn(uuid2);

    // Player 1 is not a member, player 2 is an island member
    when(island.isAllowed(any(User.class), any()))
      .thenAnswer(
        (Answer<Boolean>) invocation ->
          invocation.getArgument(0, User.class).getUniqueId().equals(uuid2)
      );

    // Create vehicle and put two players in it. One is a member, the other is not
    Vehicle vehicle = mock(Vehicle.class);
    List<Entity> passengers = new ArrayList<>();
    passengers.add(player);
    passengers.add(player2);
    when(vehicle.getPassengers()).thenReturn(passengers);
    when(vehicle.getWorld()).thenReturn(world);
    // Move vehicle
    listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
    // Player should see a message and nothing should be sent to Player 2
    verify(notifier).notify(any(), anyString());
    // User should be teleported somewhere
    verify(im).homeTeleportAsync(any(), eq(player));
    // Player 2 should not be teleported
    verify(im, never()).homeTeleportAsync(any(), eq(player2));
    // Call teleport event
    PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
    // Pass to event listener
    listener.onPlayerTeleport(ev);
    // Should not be cancelled
    assertFalse(ev.isCancelled());
  }
}
