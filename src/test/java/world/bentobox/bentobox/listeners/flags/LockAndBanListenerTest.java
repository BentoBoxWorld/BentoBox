package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class LockAndBanListenerTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private Island island;
    private World world;
    private LockAndBanListener listener;
    private Location outside;
    private Location inside;
    private Notifier notifier;
    private Location inside2;
    private BukkitScheduler sch;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        world = mock(World.class);
        
        // Island world manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");
        
        when(plugin.getIWM()).thenReturn(iwm);
        
        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player player = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        User.setPlugin(plugin);
        // User and player are not op
        when(user.isOp()).thenReturn(false);
        when(player.isOp()).thenReturn(false);
        // No special perms
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        
        // Locales      
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");
        
        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Island Banned list initialization
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(X);
        when(loc.getBlockY()).thenReturn(Y);
        when(loc.getBlockZ()).thenReturn(Z);
        when(island.getCenter()).thenReturn(loc);
        when(island.getProtectionRange()).thenReturn(PROTECTION_RANGE);
        // Island is not locked by default
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);

        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        
        // Create the listener object
        listener = new LockAndBanListener();
        
        // Common from to's
        outside = mock(Location.class);
        when(outside.getWorld()).thenReturn(world);
        when(outside.getBlockX()).thenReturn(X + PROTECTION_RANGE + 1);
        when(outside.getBlockY()).thenReturn(Y);
        when(outside.getBlockZ()).thenReturn(Z);
        
        inside = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);
        
        inside2 = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);
        
        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(inside2))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(outside))).thenReturn(Optional.empty());
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
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.any());
    }
    
    @Test
    public void testTeleportToBannedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        // Add player to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);
        
        // Simulate a teleport into an island
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should be cancelled
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.any());
    }

    @Test
    public void testLoginToBannedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        
        // Add player to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);
        
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
        // User should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere 
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
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
        Mockito.verify(im, Mockito.never()).getProtectedIslandAt(Mockito.any());
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
        Mockito.verify(im, Mockito.never()).getProtectedIslandAt(Mockito.any());
    }
    
    @Test
    public void testPlayerMoveIntoBannedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        
        // Add player to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should NOT be teleported somewhere
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
    
    @Test
    public void testPlayerMoveInsideBannedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        
        // Add player to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);        
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere
        Mockito.verify(sch).runTask(Mockito.any(), Mockito.any(Runnable.class));
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
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        
        // Add player to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);

        // Add the user to the ban list
        when(island.isBanned(Mockito.eq(uuid))).thenReturn(true);
        
        // Create vehicle and put two players in it. One is banned, the other is not
        Vehicle vehicle = mock(Vehicle.class);
        Player player2 = mock(Player.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(player);
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        // Move vehicle
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // Player should see a message and nothing should be sent to Player 2
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
        // Player 2 should not be teleported
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player2));
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
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);
        // Simulate a teleport into an island
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, outside, inside);
        // Pass to event listener
        listener.onPlayerTeleport(e);
        // Should be cancelled
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.any());
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
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.any());
    }

    @Test
    public void testLoginToLockedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);
        
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
        // User should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere 
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
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
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);
        
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
        // User should not see a message
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
        // User should not be teleported somewhere 
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
    
    @Test
    public void testLoginToLockedIslandWithBypassPerm() {
        // Make player
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);
        
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
        // User should not see a message
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
        // User should not be teleported somewhere 
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
    
    @Test
    public void testLoginToLockedIslandAsMember() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player on the island
        when(player.getLocation()).thenReturn(inside);
        // Log them in
        listener.onPlayerLogin(new PlayerJoinEvent(player, "join message"));
        // User should not see a message
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
        // User should not be teleported somewhere 
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
        
    @Test
    public void testPlayerMoveIntoLockedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should NOT be teleported somewhere
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
    
    @Test
    public void testPlayerMoveIntoLockedIslandAsOp() {
        // Make player
        Player player = mock(Player.class);
        when(player.isOp()).thenReturn(true);
        
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

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
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

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
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player just outside island
        when(player.getLocation()).thenReturn(outside);
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, outside, inside);
        listener.onPlayerMove(e);
        // Should not be cancelled
        assertFalse(e.isCancelled());
        // Player should not see a message
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
        // User should NOT be teleported somewhere
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
    
    @Test
    public void testPlayerMoveInsideLockedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertTrue(e.isCancelled());
        // Player should see a message
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere
        Mockito.verify(sch).runTask(Mockito.any(), Mockito.any(Runnable.class));
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
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

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
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        
        // Lock island for player
        when(island.isAllowed(Mockito.any(), Mockito.eq(Flags.LOCK))).thenReturn(false);

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
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Place the player inside island
        when(player.getLocation()).thenReturn(inside);
        // Move player
        PlayerMoveEvent e = new PlayerMoveEvent(player, inside, inside2);
        listener.onPlayerMove(e);
        assertFalse(e.isCancelled());
        // Player should not see a message
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
        // User should not be teleported somewhere
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player));
    }
 
    @Test
    public void testVehicleMoveIntoLockedIsland() {
        // Make player
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        Player player2 = mock(Player.class);
        UUID uuid2 = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(uuid2);
        
        // Player 1 is not a member, player 2 is an island member
        when(island.isAllowed(Mockito.any(User.class), Mockito.any())).thenAnswer((Answer<Boolean>) invocation -> invocation.getArgumentAt(0, User.class).getUniqueId().equals(uuid2));
       
        // Create vehicle and put two players in it. One is a member, the other is not
        Vehicle vehicle = mock(Vehicle.class);
        List<Entity> passengers = new ArrayList<>();
        passengers.add(player);
        passengers.add(player2);
        when(vehicle.getPassengers()).thenReturn(passengers);
        // Move vehicle
        listener.onVehicleMove(new VehicleMoveEvent(vehicle, outside, inside));
        // Player should see a message and nothing should be sent to Player 2
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.anyString());
        // User should be teleported somewhere
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
        // Player 2 should not be teleported
        Mockito.verify(im, Mockito.never()).homeTeleport(Mockito.any(), Mockito.eq(player2));
        // Call teleport event
        PlayerTeleportEvent ev = new PlayerTeleportEvent(player, inside, outside);
        // Pass to event listener
        listener.onPlayerTeleport(ev);
        // Should not be cancelled
        assertFalse(ev.isCancelled());
    }
    
}
