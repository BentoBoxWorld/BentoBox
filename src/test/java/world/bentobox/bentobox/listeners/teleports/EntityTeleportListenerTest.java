package world.bentobox.bentobox.listeners.teleports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.entity.EntityPortalEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class EntityTeleportListenerTest extends CommonTestSetup {

    private EntityTeleportListener etl;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));

        etl = new EntityTeleportListener(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#EntityTeleportListener(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    void testEntityTeleportListener() {
        assertNotNull(etl);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalWrongWorld() {
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(null);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalWrongWorld2() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalNullTo() {
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, null, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalTeleportDisabled() {
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalTeleportEnabled() {
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalTeleportEnabledMissingWorld() {
        when(iwm.isNetherGenerate(any())).thenReturn(false);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);
        
        when(location.getWorld()).thenReturn(world);
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalTeleportEnabledIsNotAllowedInConfig() {
        when(iwm.isNetherGenerate(any())).thenReturn(false);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    void testOnEntityPortalTeleportEnabledIsAllowedInConfig() {
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(true);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityEnterPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    void testOnEntityEnterPortal() {
        // TODO
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityExitPortal(org.bukkit.event.entity.EntityPortalExitEvent)}.
     */
    @Test
    void testOnEntityExitPortal() {
        // TODO
    }

    /**
     * On Paper the native transfer continues unless the event is cancelled, so an entity
     * heading to a dimension that has no island yet must be stopped (it cannot paste one).
     */
    @Test
    void testEntityTeleportCancelledWhenThePartnerIslandIsMissingOnPaper() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        mockedBukkit.when(Bukkit::getAllowNether).thenReturn(true);
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        when(iwm.isPasteMissingIslands(world)).thenReturn(true);
        when(iwm.isUseOwnGenerator(world)).thenReturn(false);
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        when(island.hasNetherIsland()).thenReturn(false);

        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, new Location(nether, 0, 64, 0), 10);
        etl.onEntityPortal(event);

        assertTrue(event.isCancelled());
    }

    /**
     * An entity cannot generate a missing island, but it must not be left standing
     * in the portal either. The transfer has to be completed to the computed
     * location so the entity reaches the other dimension.
     */
    @Test
    void testEntityIsTransferredWhenThePartnerIslandIsMissing() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        mockedBukkit.when(Bukkit::getAllowNether).thenReturn(true);
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        // The addon pastes missing islands, so the nether island may be absent.
        when(iwm.isPasteMissingIslands(world)).thenReturn(true);
        when(iwm.isUseOwnGenerator(world)).thenReturn(false);
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        // The nether island of this island does not exist yet.
        when(island.hasNetherIslandAsync()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));
        // The safe spot search is the observable sign that the transfer was started.
        mockedUtil.when(() -> Util.getChunkAtAsync(any()))
                .thenReturn(java.util.concurrent.CompletableFuture
                        .failedFuture(new IllegalStateException("test stops the search")));
        // Run the scheduled work straight away, the mocked scheduler does not tick.
        doAnswer(call -> {
            ((Runnable) call.getArgument(1)).run();
            return null;
        }).when(sch).runTask(any(), any(Runnable.class));
        // Deliver the event to the listener the way the event bus would.
        doAnswer(call -> {
            etl.onEntityPortal((EntityPortalEvent) call.getArgument(0));
            return null;
        }).when(pim).callEvent(any(EntityPortalEvent.class));

        Location netherLocation = new Location(nether, 0, 64, 0);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, netherLocation, 10);
        AbstractTeleportListener.dispatchPortal(event, false);

        assertFalse(event.isCancelled());
        mockedUtil.verify(() -> Util.getChunkAtAsync(any()));
    }

}
