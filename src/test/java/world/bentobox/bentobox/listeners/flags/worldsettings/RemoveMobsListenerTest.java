package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class RemoveMobsListenerTest {

    @Mock
    private IslandsManager im;
    @Mock
    private World world;
    @Mock
    private Location inside;
    @Mock
    private Location outside;
    @Mock
    private Player player;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private Settings settings;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getClearRadius()).thenReturn(10);

        // Owner
        UUID uuid1 = UUID.randomUUID();

        // Island initialization
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid1);

        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);

        // Location
        when(inside.getWorld()).thenReturn(world);
        // Teleports are from far away
        when(inside.distanceSquared(any())).thenReturn(100D);


        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        // On island
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // World Settings
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        Flags.REMOVE_MOBS.setSetting(world, true);
        when(iwm.inWorld(world)).thenReturn(true);

        // Sometimes use Mockito.withSettings().verboseLogging()
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);

        // Scheduler
        when(Bukkit.getScheduler()).thenReturn(scheduler);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportDifferentWorld() {
        when(player.getWorld()).thenReturn(mock(World.class));
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportChorusEtc() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
        new RemoveMobsListener().onUserTeleport(e);
        e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        new RemoveMobsListener().onUserTeleport(e);
        e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.SPECTATE);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportTooClose() {
        // Teleports are too close
        when(inside.distanceSquared(any())).thenReturn(10D);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportDoNotRemove() {
        Flags.REMOVE_MOBS.setSetting(world, false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportToNotIsland() {
        // Not on island
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawn() {
        PlayerRespawnEvent e = new PlayerRespawnEvent(player, inside, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(scheduler).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawnDoNotRemove() {
        Flags.REMOVE_MOBS.setSetting(world, false);
        PlayerRespawnEvent e = new PlayerRespawnEvent(player, inside, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawnNotIsland() {
        // Not on island
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        PlayerRespawnEvent e = new PlayerRespawnEvent(player, inside, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(scheduler, never()).runTask(any(), any(Runnable.class));
    }
}
