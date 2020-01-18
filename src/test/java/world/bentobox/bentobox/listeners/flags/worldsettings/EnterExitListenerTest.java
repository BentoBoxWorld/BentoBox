package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
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

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandEnterEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandExitEvent;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class })
public class EnterExitListenerTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    @Mock
    private User user;
    @Mock
    private Island island;
    @Mock
    private Location outside;
    @Mock
    private Location inside;
    @Mock
    private Location anotherWorld;
    @Mock
    private LocalesManager lm;
    @Mock
    private World world;
    @Mock
    private PluginManager pim;
    @Mock
    private Notifier notifier;

    private EnterExitListener listener;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Server
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        User.setPlugin(plugin);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");

        // No island for player to begin with (set it later in the tests)
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);

        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Island initialization
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(X);
        when(loc.getBlockY()).thenReturn(Y);
        when(loc.getBlockZ()).thenReturn(Z);
        when(island.getCenter()).thenReturn(loc);
        when(island.getProtectionRange()).thenReturn(PROTECTION_RANGE);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));
        when(island.isOwned()).thenReturn(true);

        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // Common from to's
        when(outside.getWorld()).thenReturn(world);
        when(outside.getBlockX()).thenReturn(X + PROTECTION_RANGE + 1);
        when(outside.getBlockY()).thenReturn(Y);
        when(outside.getBlockZ()).thenReturn(Z);
        when(outside.toVector()).thenReturn(new Vector(X + PROTECTION_RANGE + 1, Y, Z));

        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);
        when(inside.toVector()).thenReturn(new Vector(X + PROTECTION_RANGE - 1, Y, Z));

        Location inside2 = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);
        when(inside.toVector()).thenReturn(new Vector(X + PROTECTION_RANGE -2, Y, Z));

        // Same as inside, but another world
        when(anotherWorld.getWorld()).thenReturn(mock(World.class));
        when(anotherWorld.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(anotherWorld.getBlockY()).thenReturn(Y);
        when(anotherWorld.getBlockZ()).thenReturn(Z);
        when(anotherWorld.toVector()).thenReturn(new Vector(X + PROTECTION_RANGE - 1, Y, Z));

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(eq(inside2))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(eq(outside))).thenReturn(Optional.empty());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player's manager
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // Listener
        listener = new EnterExitListener();

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Flags
        Flags.ENTER_EXIT_MESSAGES.setSetting(world, true);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnMoveInsideIsland() {
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), inside, inside);
        listener.onMove(e);
        // Moving in the island should result in no messages to the user
        verify(notifier, never()).notify(any(), any());
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnMoveOutsideIsland() {
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), outside, outside);
        listener.onMove(e);
        // Moving outside the island should result in no messages to the user
        verify(notifier, never()).notify(any(), any());
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnGoingIntoIslandEmptyIslandName() {
        when(island.getName()).thenReturn("");
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), outside, inside);
        listener.onMove(e);
        // Moving into the island should show a message
        verify(lm).get(any(), eq("protection.flags.ENTER_EXIT_MESSAGES.now-entering"));
        // The island owner needs to be checked
        verify(island).isOwned();
        verify(pim).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-entering"));
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnGoingIntoIslandWithIslandName() {
        when(island.getName()).thenReturn("fancy name");
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), outside, inside);
        listener.onMove(e);
        // Moving into the island should show a message
        verify(lm).get(any(), eq("protection.flags.ENTER_EXIT_MESSAGES.now-entering"));
        // No owner check
        verify(island).isOwned();
        verify(island, times(2)).getName();
        verify(pim).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-entering"));
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testExitingIslandEmptyIslandName() {
        when(island.getName()).thenReturn("");
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), inside, outside);
        listener.onMove(e);
        // Moving into the island should show a message
        verify(lm).get(any(), eq("protection.flags.ENTER_EXIT_MESSAGES.now-leaving"));
        // The island owner needs to be checked
        verify(island).isOwned();
        verify(pim).callEvent(any(IslandExitEvent.class));
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-leaving"));
    }

    /**
     * Test method for {@link EnterExitListener#onMove(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testExitingIslandWithIslandName() {
        when(island.getName()).thenReturn("fancy name");
        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), inside, outside);
        listener.onMove(e);
        // Moving into the island should show a message
        verify(lm).get(any(), eq("protection.flags.ENTER_EXIT_MESSAGES.now-leaving"));
        // No owner check
        verify(island).isOwned();
        verify(island, times(2)).getName();
        verify(pim).callEvent(any(IslandExitEvent.class));
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-leaving"));
    }

    /**
     * Asserts that no notifications are sent if {@link world.bentobox.bentobox.lists.Flags#ENTER_EXIT_MESSAGES Flags#ENTER_EXIT_MESSAGES} flag is set to false.
     * @since 1.4.0
     */
    @Test
    public void testNoNotificationIfDisabled() {
        // No notifications should be sent
        Flags.ENTER_EXIT_MESSAGES.setSetting(world, false);

        PlayerMoveEvent e = new PlayerMoveEvent(user.getPlayer(), inside, outside);
        listener.onMove(e);
        // No messages should be sent
        verify(notifier, never()).notify(any(), any());
        // Still send event
        verify(pim).callEvent(any(IslandExitEvent.class));
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
    }

    /**
     * Test method for {@link EnterExitListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testEnterIslandTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(user.getPlayer(), anotherWorld, inside, TeleportCause.PLUGIN);
        listener.onTeleport(e);
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-entering"));
        verify(pim).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
    }

    /**
     * Test method for {@link EnterExitListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testExitIslandTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(user.getPlayer(), inside, anotherWorld, TeleportCause.PLUGIN);
        listener.onTeleport(e);
        verify(notifier).notify(any(User.class), eq("protection.flags.ENTER_EXIT_MESSAGES.now-leaving"));
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(pim).callEvent(any(IslandExitEvent.class));
    }


    /**
     * Test method for {@link EnterExitListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testEnterIslandTeleportUnowned() {
        when(island.isOwned()).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(user.getPlayer(), anotherWorld, inside, TeleportCause.PLUGIN);
        listener.onTeleport(e);
        verify(notifier, never()).notify(any(User.class), anyString());
        verify(pim).callEvent(any(IslandEnterEvent.class));
        verify(pim, never()).callEvent(any(IslandExitEvent.class));
    }

    /**
     * Test method for {@link EnterExitListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testExitIslandTeleportUnowned() {
        when(island.isOwned()).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(user.getPlayer(), inside, anotherWorld, TeleportCause.PLUGIN);
        listener.onTeleport(e);
        verify(notifier, never()).notify(any(User.class), anyString());
        verify(pim, never()).callEvent(any(IslandEnterEvent.class));
        verify(pim).callEvent(any(IslandExitEvent.class));
    }

    // TODO add tests to make sure the enter/exit messages work properly when on an island the player is part of.
}
