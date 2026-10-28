package world.bentobox.bentobox.listeners.teleports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Tests the decisions {@link FoliaPortalBridge} makes about Luminol's pre-portal event.
 */
class FoliaPortalBridgeTest extends CommonTestSetup {

    /**
     * Stand-in for Luminol's PreEntityPortalEvent. The bridge reads the event reflectively,
     * so any class with the same three getters and Cancellable can be used here.
     */
    public static class FakePortalEvent extends Event implements Cancellable {
        private final Entity entity;
        private final Location portalPos;
        private World destination;
        private boolean cancelled;
        private Location portalTarget;
        private int searchRadius;
        private int creationRadius;
        private java.util.function.Supplier<java.util.concurrent.CompletableFuture<Boolean>> portalReady;

        public void setPortalTarget(Location target) { portalTarget = target; }
        public void setSearchRadius(int radius) { searchRadius = radius; }
        public void setCreationRadius(int radius) { creationRadius = radius; }
        public void setPortalReady(java.util.function.Supplier<java.util.concurrent.CompletableFuture<Boolean>> ready) {
            portalReady = ready;
        }

        FakePortalEvent(Entity entity, Location portalPos, World destination) {
            this.entity = entity;
            this.portalPos = portalPos;
            this.destination = destination;
        }

        public Entity getEntity() {
            return entity;
        }

        public Location getPortalPos() {
            return portalPos;
        }

        public World getDestination() {
            return destination;
        }

        public void setDestination(World destination) {
            this.destination = destination;
        }


        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return getHandlerList();
        }

        public static HandlerList getHandlerList() {
            return new HandlerList();
        }
    }

    private PlayerTeleportListener ptl;
    private Method getEntity;
    private Method getPortalPos;
    private Method getDestination;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ptl = new PlayerTeleportListener(plugin);
        getEntity = FakePortalEvent.class.getMethod("getEntity");
        getPortalPos = FakePortalEvent.class.getMethod("getPortalPos");
        getDestination = FakePortalEvent.class.getMethod("getDestination");
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
        when(iwm.inWorld(world)).thenReturn(true);
        mockedBukkit.when(Bukkit::getAllowNether).thenReturn(true);
        mockedBukkit.when(Bukkit::getAllowEnd).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
    }

    private FakePortalEvent reroute(World destination) {
        FakePortalEvent event = new FakePortalEvent(mockPlayer, location, destination);
        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination);
        return event;
    }

    /**
     * Makes the island-portal path settle at once: the paired-portal lookup finds nothing and the
     * entity scheduler runs the dispatch inline, so the assertions can read the event right away.
     */
    private PlayerTeleportListener islandPathRunsInline(Entity entity) {
        return islandPathRunsInline(spyWithPortals(), entity);
    }

    private PlayerTeleportListener islandPathRunsInline(PlayerTeleportListener listener, Entity entity) {
        // The bridge logs per entity and a null id would throw inside its guarded body.
        org.mockito.Mockito.doReturn(java.util.UUID.randomUUID()).when(entity).getUniqueId();
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
                .when(listener).ensureNetherPortal(org.mockito.ArgumentMatchers.any(Location.class),
                        org.mockito.ArgumentMatchers.any(org.bukkit.Axis.class),
                        org.mockito.ArgumentMatchers.anyBoolean());
        var scheduler = mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(scheduler);
        when(scheduler.execute(org.mockito.ArgumentMatchers.eq(plugin),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(1L))).thenAnswer(call -> {
                    call.getArgument(1, Runnable.class).run();
                    return true;
                });
        return listener;
    }

    @Test
    void testLateSyntheticCancellationStopsNativeTargetAndPreparation() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        doAnswer(call -> {
            ((PlayerPortalEvent) call.getArgument(0)).setCancelled(true);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(PlayerPortalEvent.class));
        FakePortalEvent event = new FakePortalEvent(mockPlayer, new Location(world, 12, 80, 34), nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertTrue(event.isCancelled());
        org.junit.jupiter.api.Assertions.assertNull(event.portalTarget);
        org.junit.jupiter.api.Assertions.assertNull(event.portalReady);
        org.mockito.Mockito.verify(listener, org.mockito.Mockito.never()).getIsland(org.mockito.ArgumentMatchers.any(Location.class));
    }

    @Test
    void testNativeFinalPlayerTargetAndRadiiAreForwarded() throws Exception {
        World nether = mock(World.class);
        World redirected = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        Location finalTarget = new Location(redirected, 6100.5, 73, -9200.5);
        doAnswer(call -> {
            PlayerPortalEvent portal = call.getArgument(0);
            portal.setTo(finalTarget);
            portal.setSearchRadius(7);
            portal.setCreationRadius(3);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(PlayerPortalEvent.class));
        FakePortalEvent event = new FakePortalEvent(mockPlayer, new Location(world, 12, 80, 34), nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertFalse(event.isCancelled());
        assertEquals(redirected, event.destination);
        assertEquals(finalTarget, event.portalTarget);
        assertEquals(7, event.searchRadius);
        assertEquals(3, event.creationRadius);
    }

    @Test
    void testNativeMissingIslandPreparationIsLazyAndHonorsLateNativeCancellation() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener).getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Location from = new Location(world, 10, 80, 20);
        when(mockPlayer.getLocation()).thenReturn(from);
        var checked = new java.util.concurrent.CompletableFuture<Boolean>();
        org.mockito.Mockito.doReturn(checked).when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertFalse(event.isCancelled());
        org.mockito.Mockito.verify(listener, org.mockito.Mockito.never()).hasPartnerIsland(island, World.Environment.NETHER);
        event.setCancelled(true);
        assertFalse(event.portalReady.get().join());
        org.mockito.Mockito.verify(listener, org.mockito.Mockito.never()).hasPartnerIsland(island, World.Environment.NETHER);
    }

    @Test
    void testNativeMissingIslandPastesOnceThenResumesOnEntityOwner() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener).getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Location from = new Location(world, 10, 80, 20);
        Location target = new Location(nether, 10, 80, 20);
        when(mockPlayer.getLocation()).thenReturn(from);
        when(mockPlayer.isValid()).thenReturn(true);
        when(mockPlayer.getWorld()).thenReturn(world);
        var scheduler = mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(scheduler);
        Runnable[] resume = {null};
        when(scheduler.execute(org.mockito.ArgumentMatchers.eq(plugin), org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenAnswer(call -> { resume[0] = call.getArgument(1); return true; });
        var checked = new java.util.concurrent.CompletableFuture<Boolean>();
        var pasted = new java.util.concurrent.CompletableFuture<Void>();
        when(island.getUniqueId()).thenReturn("native-missing-test");
        org.mockito.Mockito.doReturn(checked).when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(pasted).when(listener).pasteMissingIsland(target, island, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        var ready = event.portalReady.get();
        FakePortalEvent duplicate = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, duplicate, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertTrue(duplicate.isCancelled());
        checked.complete(false);
        verify(listener).pasteMissingIsland(target, island, World.Environment.NETHER);
        assertFalse(ready.isDone());
        pasted.complete(null);
        assertFalse(ready.isDone());
        resume[0].run();
        assertTrue(ready.join());
        verify(pim, org.mockito.Mockito.times(1)).callEvent(org.mockito.ArgumentMatchers.any(PlayerPortalEvent.class));
    }

    /**
     * An entity cannot paste an island. A missing partner dimension must therefore refuse
     * the transfer quietly: the native event fires again every tick while the entity stays
     * in the portal, so a stack trace per attempt would flood the log.
     */
    @Test
    void testNativeEntityMissingIslandIsRefusedWithoutLogging() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener)
                .getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Entity cow = mock(Entity.class);
        when(cow.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(cow.getWorld()).thenReturn(world);
        var scheduler = mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
        when(cow.getScheduler()).thenReturn(scheduler);
        when(scheduler.execute(org.mockito.ArgumentMatchers.eq(plugin), org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenAnswer(call -> {
                    ((Runnable) call.getArgument(1)).run();
                    return true;
                });
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(false))
                .when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(cow, new Location(world, 1, 70, 2), nether);

        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertFalse(event.portalReady.get().join());
        verify(plugin, org.mockito.Mockito.never()).logStacktrace(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(listener, org.mockito.Mockito.never()).pasteOnce(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testNativeMissingIslandPreparationTimesOutAndReleasesEntity() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener)
                .getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Location from = new Location(world, 10, 80, 20);
        when(mockPlayer.getLocation()).thenReturn(from);
        org.mockito.Mockito.doReturn(new java.util.concurrent.CompletableFuture<Boolean>())
                .when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        Runnable[] timeout = {null};
        when(sch.runTaskLaterAsynchronously(org.mockito.ArgumentMatchers.eq(plugin),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(call -> { timeout[0] = call.getArgument(1); return null; });
        FakePortalEvent event = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        var ready = event.portalReady.get();
        assertFalse(ready.isDone());
        org.junit.jupiter.api.Assertions.assertNotNull(timeout[0]);

        timeout[0].run();

        assertFalse(ready.join());
        // The entity is released, so a later portal trip is not cancelled by a stuck preparation.
        FakePortalEvent retry = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, retry, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertFalse(retry.isCancelled());
    }

    @Test
    void testNativeMissingIslandResumesAfterEntityMoved() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener).getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Location from = new Location(world, 10, 80, 20);
        when(mockPlayer.getLocation()).thenReturn(from);
        when(mockPlayer.isValid()).thenReturn(true);
        when(mockPlayer.getWorld()).thenReturn(world);
        var scheduler = mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(scheduler);
        Runnable[] resume = {null};
        when(scheduler.execute(org.mockito.ArgumentMatchers.eq(plugin), org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenAnswer(call -> { resume[0] = call.getArgument(1); return true; });
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(true))
                .when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        var ready = event.portalReady.get();
        // A walking player leaves the one-block start radius while the async check runs.
        when(mockPlayer.getLocation()).thenReturn(from.clone().add(5, 0, 5));
        resume[0].run();
        assertTrue(ready.join());
    }

    @Test
    void testNativeMissingIslandAbortsWhenEntityIsRetired() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        org.mockito.Mockito.doReturn(true).when(listener).isPastingMissingIslands(world);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        org.mockito.Mockito.doReturn(java.util.Optional.of(island)).when(listener).getIsland(org.mockito.ArgumentMatchers.any(Location.class));
        Location from = new Location(world, 10, 80, 20);
        when(mockPlayer.getLocation()).thenReturn(from);
        when(mockPlayer.isValid()).thenReturn(false);
        when(mockPlayer.getWorld()).thenReturn(world);
        var scheduler = mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(scheduler);
        Runnable[] resume = {null};
        when(scheduler.execute(org.mockito.ArgumentMatchers.eq(plugin), org.mockito.ArgumentMatchers.any(Runnable.class),
                org.mockito.ArgumentMatchers.any(Runnable.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenAnswer(call -> { resume[0] = call.getArgument(1); return true; });
        org.mockito.Mockito.doReturn(java.util.concurrent.CompletableFuture.completedFuture(true))
                .when(listener).hasPartnerIsland(island, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, from, nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        var ready = event.portalReady.get();
        resume[0].run();
        assertFalse(ready.join());
    }

    @Test
    void testNativeSearchStillAllowedWhenCreationIsDisabled() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        doAnswer(call -> {
            PlayerPortalEvent portal = call.getArgument(0);
            portal.setCanCreatePortal(false);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(PlayerPortalEvent.class));
        FakePortalEvent event = new FakePortalEvent(mockPlayer, new Location(world, 1, 70, 2), nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertFalse(event.isCancelled());
        assertEquals(-1, event.creationRadius);
    }

    @Test
    void testNativeFinalEntityTargetAndRadiusAreForwarded() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        World nether = mock(World.class);
        World redirected = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener listener = spyWithPortals();
        Location target = new Location(redirected, 1200.5, 80, -3400.5);
        doAnswer(call -> {
            EntityPortalEvent portal = call.getArgument(0);
            portal.setTo(target);
            portal.setSearchRadius(9);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));
        Entity entity = mock(Entity.class);
        listener = islandPathRunsInline(listener, entity);
        FakePortalEvent event = new FakePortalEvent(entity, new Location(world, 1, 70, 2), nether);
        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertFalse(event.isCancelled());
        assertEquals(redirected, event.destination);
        assertEquals(target, event.portalTarget);
        assertEquals(9, event.searchRadius);
    }

    @Test
    void testSyntheticCommitWaitsForFinalCancellationAndUsesFinalTarget() {
        PlayerPortalEvent portal = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL);
        int[] commits = {0};
        doAnswer(call -> {
            ptl.afterPortal(portal, mockPlayer, () -> commits[0]++);
            assertEquals(0, commits[0]);
            portal.setCancelled(true);
            return null;
        }).when(pim).callEvent(portal);
        AbstractTeleportListener.dispatchPortal(portal, false);
        assertEquals(0, commits[0]);

        Location finalTarget = new Location(world, 200, 90, 300);
        doAnswer(call -> {
            portal.setCancelled(false);
            ptl.afterPortal(portal, mockPlayer, () -> {
                assertEquals(finalTarget, portal.getTo());
                commits[0]++;
            });
            assertEquals(0, commits[0]);
            portal.setTo(finalTarget);
            return null;
        }).when(pim).callEvent(portal);
        AbstractTeleportListener.dispatchPortal(portal, false);
        assertEquals(1, commits[0]);
    }

    @Test
    void testFoliaApiIsReportedMissingWhenNotOnClasspath() {
        assertFalse(FoliaPortalBridge.isSupported());
    }

    @Test
    void testAddonNetherTransferIsCancelledAndRerouted() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);

        FakePortalEvent event = reroute(nether);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, portalEvent.getCause());
        assertEquals(location, portalEvent.getFrom());
    }


    @Test
    void testAddonEndTransferIsCancelledAndRerouted() {
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);

        FakePortalEvent event = reroute(end);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.END_PORTAL, portalEvent.getCause());
    }

    @Test
    void testLeavingTheAddonEndIsReroutedAsAnEndPortal() {
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        mockedUtil.when(() -> Util.getWorld(end)).thenReturn(world);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);
        Location endPortal = mock(Location.class);
        when(endPortal.getWorld()).thenReturn(end);
        // The server's destination for an end portal in the end is the normal overworld.
        World serverOverworld = mock(World.class);
        when(serverOverworld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, endPortal, serverOverworld);

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.END_PORTAL, portalEvent.getCause());
    }

    @Test
    void testEnvironmentIsTakenFromPortalAndDestination() {
        World overworld = mock(World.class);
        when(overworld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);

        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(overworld, nether));
        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(nether, overworld));
        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(end, nether));
        assertEquals(World.Environment.THE_END, FoliaPortalBridge.environmentOf(overworld, end));
        assertEquals(World.Environment.THE_END, FoliaPortalBridge.environmentOf(end, overworld));
    }

    @Test
    void testEndEntityUsesIslandEventAndHonorsCancellation() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.getEndWorld(world)).thenReturn(end);
        Entity entity = mock(Entity.class);
        Location entrance = new Location(world, 1200, 90, 3400);
        doAnswer(call -> {
            ((EntityPortalEvent) call.getArgument(0)).setCancelled(true);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));
        PlayerTeleportListener listener = islandPathRunsInline(entity);
        FakePortalEvent event = new FakePortalEvent(entity, entrance, end);

        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertTrue(event.isCancelled());
        ArgumentCaptor<EntityPortalEvent> captor = ArgumentCaptor.forClass(EntityPortalEvent.class);
        verify(pim).callEvent(captor.capture());
        assertEquals(new Location(end, 1200, 90, 3400), captor.getValue().getTo());
        org.mockito.Mockito.verify(entity, org.mockito.Mockito.never()).teleportAsync(
                org.mockito.ArgumentMatchers.any(Location.class));
    }

    @Test
    void testEndEnterHonorsCancellationAndCooldown() {
        Entity entity = mock(Entity.class);
        var event = new org.bukkit.event.entity.EntityPortalEnterEvent(entity, location, org.bukkit.PortalType.ENDER);
        event.setCancelled(true);
        FoliaPortalBridge.handleEndEnter(plugin, ptl, event);
        verifyNoInteractions(entity, pim);

        event.setCancelled(false);
        when(entity.getPortalCooldown()).thenReturn(20);
        FoliaPortalBridge.handleEndEnter(plugin, ptl, event);
        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
        org.mockito.Mockito.verify(entity, org.mockito.Mockito.never()).setPortalCooldown(300);
    }

    @Test
    void testMissingEndTargetDoesNotLeakToDefaultWorld() {
        when(iwm.isEndGenerate(world)).thenReturn(true);
        Entity entity = mock(Entity.class);
        var event = new org.bukkit.event.entity.EntityPortalEnterEvent(entity, location, org.bukkit.PortalType.ENDER);
        FoliaPortalBridge.handleEndEnter(plugin, ptl, event);
        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
    }

    @Test
    void testStandardNetherIsReroutedThroughBentoBox() {
        // islands: false - the nether is a single shared world, but BentoBox still decides
        // which world that is, so the transfer must not be left to the server.
        when(iwm.isNetherIslands(world)).thenReturn(false);

        FakePortalEvent event = reroute(mock(World.class));

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, portalEvent.getCause());
    }

    @Test
    void testDisabledDimensionIsCancelledWithoutTeleport() {
        when(iwm.isNetherGenerate(world)).thenReturn(false);

        FakePortalEvent event = reroute(mock(World.class));

        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
    }

    @Test
    void testServerDisabledDimensionIsCancelledWithoutTeleport() {
        mockedBukkit.when(Bukkit::getAllowNether).thenReturn(false);

        FakePortalEvent event = reroute(mock(World.class));

        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
    }

    private PlayerTeleportListener spyWithPortals() {
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, World.Environment.NETHER);
        return listener;
    }

    @Test
    void testNativePathChangesDestinationWithoutCancelling() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        World nether = mock(World.class);
        World addonNether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(addonNether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(addonNether);
        PlayerTeleportListener listener = spyWithPortals();

        Entity nativeEntity = mock(Entity.class);
        when(nativeEntity.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        FakePortalEvent event = new FakePortalEvent(nativeEntity, location, nether);
        FoliaPortalBridge.reroute(plugin, listener, event,
                getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertFalse(event.isCancelled());
        assertEquals(addonNether, event.getDestination());
    }

    @Test
    void testNativeEntityReturnsToAddonOverworld() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        World nether = mock(World.class);
        World serverOverworld = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(serverOverworld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        mockedUtil.when(() -> Util.getWorld(nether)).thenReturn(world);
        PlayerTeleportListener listener = spyWithPortals();
        Location portal = new Location(nether, 8, 90, 8);
        Entity nativeEndEntity = mock(Entity.class);
        when(nativeEndEntity.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        FakePortalEvent event = new FakePortalEvent(nativeEndEntity, portal, serverOverworld);

        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertFalse(event.isCancelled());
        assertEquals(world, event.getDestination());
        verifyNoInteractions(pim);
    }

    @Test
    void testPlayerStillUsesIslandRoutingWhenNativeSetterExists() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, location, nether);

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(mockPlayer, portalEvent.getPlayer());
        assertEquals(location, portalEvent.getFrom());
        assertEquals(TeleportCause.NETHER_PORTAL, portalEvent.getCause());
    }

    @Test
    void testLinkingPlayerLeavesNativePortalSearchEnabled() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        PlayerTeleportListener nativeListener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(nativeListener).isMakePortals(world, World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, new Location(world, 1234, 90, 5678), nether);

        FoliaPortalBridge.reroute(plugin, nativeListener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertFalse(event.isCancelled());
        assertEquals(nether, event.getDestination());
        verify(pim).callEvent(org.mockito.ArgumentMatchers.any(PlayerPortalEvent.class));
    }

    @Test
    void testAlreadyCancelledNativePortalIsNotProcessed() throws Exception {
        FakePortalEvent event = new FakePortalEvent(mockPlayer, location, world);
        event.setCancelled(true);
        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));
        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
    }

    @Test
    void testIslandEntityUsesUnscaledCoordinatesAndHonorsCancellation() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        Location entrance = new Location(world, 2701.5, 90, 4198.5);
        Entity entity = mock(Entity.class);
        PlayerTeleportListener listener = islandPathRunsInline(entity);
        FakePortalEvent event = new FakePortalEvent(entity, entrance, nether);
        doAnswer(invocation -> {
            EntityPortalEvent portal = invocation.getArgument(0);
            assertEquals(entity, portal.getEntity());
            assertEquals(new Location(nether, 2701.5, 90, 4198.5), portal.getTo());
            portal.setCancelled(true);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));

        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertTrue(event.isCancelled());
        verify(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));
        org.mockito.Mockito.verify(entity, org.mockito.Mockito.never())
                .teleportAsync(org.mockito.ArgumentMatchers.any(Location.class));
    }

    @Test
    void testNativeEntityCannotLeakIntoDefaultNetherWhenTargetMissing() throws Exception {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        FakePortalEvent event = new FakePortalEvent(mock(Entity.class), location, nether);

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertTrue(event.isCancelled());
        assertEquals(nether, event.getDestination());
        // Reading a world flag the first time publishes and saves it, which registers listeners
        // on the plugin manager, so assert that no portal event was dispatched instead.
        verify(pim, org.mockito.Mockito.never())
                .callEvent(org.mockito.ArgumentMatchers.any(Event.class));
    }

    @Test
    void testNonBentoBoxWorldIsLeftToTheServer() {
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(null);

        FakePortalEvent event = reroute(mock(World.class));

        assertFalse(event.isCancelled());
        verifyNoInteractions(pim);
    }

    /**
     * Without a native destination setter the bridge has to cancel and route the entity
     * itself; an entity gets the same island coordinates as a player.
     */
    @Test
    void testNonPlayerEntityUsesIslandRoutingWithoutNativeSetter() {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        Entity sheep = mock(Entity.class);
        PlayerTeleportListener listener = islandPathRunsInline(sheep);
        FakePortalEvent event = new FakePortalEvent(sheep, location, nether);
        doAnswer(invocation -> {
            EntityPortalEvent portal = invocation.getArgument(0);
            assertEquals(sheep, portal.getEntity());
            assertEquals(new Location(nether, location.getX(), location.getY(), location.getZ()), portal.getTo());
            portal.setCancelled(true);
            return null;
        }).when(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));

        FoliaPortalBridge.reroute(plugin, listener, event, getEntity, getPortalPos, getDestination);

        assertTrue(event.isCancelled());
        verify(pim).callEvent(org.mockito.ArgumentMatchers.any(EntityPortalEvent.class));
        // The bridge only logs on the entity; it must not move it itself.
        org.mockito.Mockito.verify(sheep, org.mockito.Mockito.never())
                .teleportAsync(org.mockito.ArgumentMatchers.any(Location.class));
    }

    /**
     * The entity flag defaults to false and Folia never fires EntityPortalEvent, so the
     * bridge has to enforce it itself. Otherwise animals, minecarts and items keep crossing
     * dimensions on Folia while Paper refuses them.
     */
    @Test
    void testNonPlayerEntityIsRefusedWhenTheEntityFlagIsOff() throws Exception {
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, false);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.getNetherWorld(world)).thenReturn(nether);

        FakePortalEvent event = new FakePortalEvent(mock(Entity.class), location, nether);

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination,
                FakePortalEvent.class.getMethod("setDestination", World.class));

        assertTrue(event.isCancelled());
        org.junit.jupiter.api.Assertions.assertNull(event.portalTarget);
        verifyNoInteractions(pim);
    }
}
