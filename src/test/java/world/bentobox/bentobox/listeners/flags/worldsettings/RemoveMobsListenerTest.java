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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class RemoveMobsListenerTest extends CommonTestSetup {

    @Mock
    private Location inside;
    @Mock
    private Location outside;
    @Mock
    private Settings settings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getClearRadius()).thenReturn(10);

        // Owner
        UUID uuid1 = UUID.randomUUID();

        // Island initialization
        when(island.getOwner()).thenReturn(uuid1);

        when(im.getIsland(any(), Mockito.any(UUID.class))).thenReturn(island);

        // Location
        when(inside.getWorld()).thenReturn(world);
        // Teleports are from far away
        when(inside.distanceSquared(any())).thenReturn(100D);

        when(inside.clone()).thenReturn(inside);


        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        // On island
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // World Settings
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        Flags.REMOVE_MOBS.setSetting(world, true);
        when(iwm.inWorld(world)).thenReturn(true);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleport() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportDifferentWorld() {
        when(mockPlayer.getWorld()).thenReturn(mock(World.class));
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportChorusEtc() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT);
        new RemoveMobsListener().onUserTeleport(e);
        e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        new RemoveMobsListener().onUserTeleport(e);
        e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.SPECTATE);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportTooClose() {
        // Teleports are too close
        when(inside.distanceSquared(any())).thenReturn(10D);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportDoNotRemove() {
        Flags.REMOVE_MOBS.setSetting(world, false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testOnUserTeleportToNotIsland() {
        // Not on island
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, inside, inside, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new RemoveMobsListener().onUserTeleport(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawn() {
        PlayerRespawnEvent e = new PlayerRespawnEvent(mockPlayer, inside, false, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(sch).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawnDoNotRemove() {
        Flags.REMOVE_MOBS.setSetting(world, false);

        PlayerRespawnEvent e = new PlayerRespawnEvent(mockPlayer, inside, false, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for {@link RemoveMobsListener#onUserRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnUserRespawnNotIsland() {
        // Not on island
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        PlayerRespawnEvent e = new PlayerRespawnEvent(mockPlayer, inside, false, false, false, RespawnReason.DEATH);
        new RemoveMobsListener().onUserRespawn(e);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }
}
