package world.bentobox.bentobox.listeners.teleports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * @author tastybento
 */
class PlayerTeleportListenerTest extends CommonTestSetup {

    private PlayerTeleportListener ptl;
    @Mock
    private Block block;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Bukkit
        when(Bukkit.getAllowNether()).thenReturn(true);
        when(Bukkit.getAllowEnd()).thenReturn(true);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.getSpawnLocation()).thenReturn(location);
        // Location
        Vector vector = mock(Vector.class);
        when(vector.toLocation(world)).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        // IWM
        when(iwm.getNetherWorld(world)).thenReturn(world);
        when(iwm.getEndWorld(world)).thenReturn(world);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);

        // Util
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);

        // IM
        when(plugin.getIslandsManager()).thenReturn(im);

        // Block
        when(location.getBlock()).thenReturn(block);

        ptl = new PlayerTeleportListener(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#PlayerTeleportListener(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    void testPlayerTeleportListener() {
        assertNotNull(ptl);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    void testOnPlayerPortalEventNether() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    void testOnPlayerPortalEventEnd() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL, 0, false,
                0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    void testOnPlayerPortalEventUnknown() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.UNKNOWN, 0, false, 0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessNotBentoboxWorld() {
        when(Util.getWorld(location.getWorld())).thenReturn(null);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that no further processing occurs
        assertFalse(e.isCancelled());
        verifyNoMoreInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessWorldDisabledInConfig() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the event was cancelled
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessWorldDisabledOnServer() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the event was cancelled
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessAlreadyInTeleport() {
        ptl.getInTeleport().add(uuid);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify no further processing occurs
        assertFalse(e.isCancelled());
    }

    @Test
    void testPortalProcessStandardNetherOrEnd() {
        // Mocking required dependencies
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, World.Environment.NETHER)).thenReturn(false);

        // Creating the event
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);

        // Running the method
        ptl.onPlayerPortalEvent(e);

        // Validating that the event destination is unchanged (indicating standard processing occurred)
        assertFalse(e.isCancelled());
        assertNotNull(e.getTo());
        assertEquals(location.getWorld(), e.getTo().getWorld());
    }

    @Test
    void testPortalProcessStandardNetherTeleportsItselfOnFolia() {
        // islands: false - the addon uses one shared nether world. BentoBox picks that world
        // and, because the native transfer is cancelled on Folia, does the teleport itself.
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(nether.getSpawnLocation()).thenReturn(new Location(nether, 0, 64, 0));
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        when(Util.getWorld(world)).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, Environment.NETHER)).thenReturn(false);
        Location from = new Location(world, 10, 80, 10);
        PlayerPortalEvent event = new PlayerPortalEvent(mockPlayer, from, from, TeleportCause.NETHER_PORTAL, 0, false,
                0);

        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            ptl.onPlayerPortalEvent(event);
            tasks.verify(() -> TaskScheduler.entity(eq(plugin), eq(mockPlayer), any(Runnable.class)), times(1));
        }

        assertFalse(event.isCancelled());
        assertSame(nether, event.getTo().getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessIslandTeleport() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, World.Environment.NETHER)).thenReturn(true);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the portal creation settings were adjusted
        assertEquals(2, e.getCreationRadius());
        assertNotNull(e.getTo());
    }

    @Test
    void testPortalProcessIslandTeleportTargetsAddonNether() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        Location from = new Location(world, 10, 80, 10);
        Location netherLocation = new Location(nether, 10, 80, 10);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        when(Util.getWorld(world)).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, Environment.NETHER)).thenReturn(true);

        PlayerPortalEvent event = new PlayerPortalEvent(mockPlayer, from, from,
                TeleportCause.NETHER_PORTAL, 0, false, 0);

        ptl.onPlayerPortalEvent(event);

        assertSame(nether, event.getTo().getWorld());
    }

    @Test
    void testPortalProcessFoliaDoesNotLetServerTeleport() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        when(Util.getWorld(world)).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, Environment.NETHER)).thenReturn(true);
        // Make the addon want portals created, so BentoBox would normally leave the transfer
        // to the server.
        GameModeAddon addon = mock(GameModeAddon.class);
        WorldSettings addonSettings = mock(WorldSettings.class);
        when(addonSettings.isMakeNetherPortals()).thenReturn(true);
        when(addon.getWorldSettings()).thenReturn(addonSettings);
        when(iwm.getAddon(world)).thenReturn(Optional.of(addon));
        Location from = new Location(world, 10, 80, 10);
        PlayerPortalEvent event = new PlayerPortalEvent(mockPlayer, from, from,
                TeleportCause.NETHER_PORTAL, 0, false, 0);

        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
            ptl.onPlayerPortalEvent(event);
            // The native transfer is cancelled on Folia, so BentoBox has to teleport itself
            tasks.verify(() -> TaskScheduler.entity(eq(plugin), eq(mockPlayer), any(Runnable.class)), times(1));
        }

        assertFalse(event.isCancelled());
        assertSame(nether, event.getTo().getWorld());
    }

    @Test
    void testHandleFoliaPortalNetherFiresPlayerPortalEvent() {
        ptl.handleFoliaPortal(mockPlayer, location, Environment.NETHER);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(eventCaptor.capture());
        PlayerPortalEvent event = assertInstanceOf(PlayerPortalEvent.class, eventCaptor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, event.getCause());
        assertEquals(location, event.getFrom());
        assertNull(event.getTo());
    }

    @Test
    void testHandleFoliaPortalEndFiresPlayerPortalEvent() {
        ptl.handleFoliaPortal(mockPlayer, location, Environment.THE_END);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(eventCaptor.capture());
        PlayerPortalEvent event = assertInstanceOf(PlayerPortalEvent.class, eventCaptor.getValue());
        assertEquals(TeleportCause.END_PORTAL, event.getCause());
        assertNull(event.getTo());
    }

    @Test
    void testEndBlueprintSpawnOverridesPortalHeightWithoutMutatingSpawn() {
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        when(end.getMaxHeight()).thenReturn(256);
        Location from = new Location(world, 796, 73, 1599);
        Location spawn = new Location(end, 799.5, 86, 1599.5, 315, 30);
        var island = mock(world.bentobox.bentobox.database.objects.Island.class);
        when(im.getProtectedIslandAt(from)).thenReturn(Optional.of(island));
        when(island.getSpawnPoint(Environment.THE_END)).thenReturn(spawn);

        assertEquals(spawn, ptl.calculateLocation(from, world, end, Environment.THE_END, true));
        Location result = ptl.calculateLocation(from, world, end, Environment.THE_END, true);
        result.setY(20);
        assertEquals(86, spawn.getY());
        EntityTeleportListener entities = new EntityTeleportListener(plugin);
        assertEquals(spawn, entities.calculateLocation(from, world, end, Environment.THE_END, true));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    void testPortalProcessEndVelocityReset() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL, 0, false,
                0);
        org.mockito.Mockito.doAnswer(call -> {
            ptl.onPlayerPortalEvent(call.getArgument(0));
            verify(mockPlayer, never()).setVelocity(any(Vector.class));
            return null;
        }).when(pim).callEvent(e);
        AbstractTeleportListener.dispatchPortal(e, false);
        // Velocity is only changed after all event listeners have returned.
        verify(mockPlayer, times(1)).setVelocity(new Vector(0, 0, 0));
        verify(mockPlayer, times(1)).setFallDistance(0);
    }

    /**
     * Verifies that {@link AbstractTeleportListener#createEndPlatform(Location)} lays a 5x5
     * obsidian floor when the destination is void in The End.
     * Regression test for AOneBlock issue #433.
     */
    @Test
    void testCreateEndPlatformBuildsFloorInVoidOfEnd() {
        World endWorld = mock(World.class);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);
        Location dest = mock(Location.class);
        when(dest.getWorld()).thenReturn(endWorld);
        when(dest.getBlockX()).thenReturn(50);
        when(dest.getBlockY()).thenReturn(70);
        when(dest.getBlockZ()).thenReturn(-30);

        Block voidSlot = mock(Block.class);
        when(voidSlot.isEmpty()).thenReturn(true);
        when(endWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(voidSlot);

        ptl.createEndPlatform(dest);

        // 5x5 = 25 obsidian blocks placed, and nothing is cleared above.
        verify(voidSlot, times(25)).setType(Material.OBSIDIAN, false);
        verify(voidSlot, never()).setType(Material.AIR, false);
    }

    /**
     * A player may already have built at the arrival coordinates. The platform must not
     * overwrite those blocks: that would delete builds and container contents.
     */
    @Test
    void testCreateEndPlatformDoesNotOverwriteExistingBlocks() {
        World endWorld = mock(World.class);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);
        Location dest = mock(Location.class);
        when(dest.getWorld()).thenReturn(endWorld);
        when(dest.getBlockX()).thenReturn(50);
        when(dest.getBlockY()).thenReturn(70);
        when(dest.getBlockZ()).thenReturn(-30);

        Block builtSlot = mock(Block.class);
        when(builtSlot.isEmpty()).thenReturn(false);
        when(builtSlot.getType()).thenReturn(Material.CHEST);
        when(endWorld.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(builtSlot);

        ptl.createEndPlatform(dest);

        verify(builtSlot, never()).setType(any(Material.class), eq(false));
    }

    /**
     * The platform helper must be a no-op outside The End — otherwise we'd corrupt blocks
     * in the overworld or nether.
     */
    @Test
    void testCreateEndPlatformDoesNothingOutsideEnd() {
        World normalWorld = mock(World.class);
        when(normalWorld.getEnvironment()).thenReturn(Environment.NORMAL);
        Location dest = mock(Location.class);
        when(dest.getWorld()).thenReturn(normalWorld);

        ptl.createEndPlatform(dest);

        verify(normalWorld, times(0)).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    @Test
    void realPaperPortalLeavesTeleportContinuationToServer() {
        when(iwm.inWorld(world)).thenReturn(true);
        PlayerPortalEvent event = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL);
        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(false);
            ptl.onPlayerPortalEvent(event);
            assertFalse(event.isCancelled());
            verify(mockPlayer, never()).teleportAsync(any(Location.class));
            tasks.verify(() -> TaskScheduler.entity(any(), any(), any()), never());
        }
    }

    @Test
    void foliaEndPlatformWaitsForFinalDecisionAndUsesRedirectedTarget() {
        when(iwm.inWorld(world)).thenReturn(true);
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        when(iwm.getEndWorld(world)).thenReturn(end);
        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        org.mockito.Mockito.doReturn(true).when(listener).isMakePortals(world, Environment.THE_END);
        Location initial = new Location(end, 1000, 70, 1000);
        Location redirected = new Location(end, 2000, 80, 2000);
        org.mockito.Mockito.doReturn(initial).when(listener).calculateLocation(
                any(Location.class), any(World.class), any(World.class), eq(Environment.THE_END), eq(true));
        org.mockito.Mockito.doReturn(new java.util.concurrent.CompletableFuture<Void>())
                .when(listener).createEndPlatformAsync(any(Location.class));
        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            PlayerPortalEvent cancelled = new PlayerPortalEvent(mockPlayer, location, initial, TeleportCause.END_PORTAL);
            org.mockito.Mockito.doAnswer(call -> {
                listener.onPlayerPortalEvent(cancelled);
                verify(listener, never()).createEndPlatformAsync(any(Location.class));
                cancelled.setCancelled(true);
                return null;
            }).when(pim).callEvent(cancelled);
            AbstractTeleportListener.dispatchPortal(cancelled, false);
            verify(listener, never()).createEndPlatformAsync(any(Location.class));
            assertFalse(listener.getInTeleport().contains(mockPlayer.getUniqueId()));

            PlayerPortalEvent accepted = new PlayerPortalEvent(mockPlayer, location, initial, TeleportCause.END_PORTAL);
            org.mockito.Mockito.doAnswer(call -> {
                listener.onPlayerPortalEvent(accepted);
                verify(listener, never()).createEndPlatformAsync(any(Location.class));
                accepted.setTo(redirected);
                return null;
            }).when(pim).callEvent(accepted);
            AbstractTeleportListener.dispatchPortal(accepted, false);
            verify(listener).createEndPlatformAsync(redirected);
        }
    }

    @Test
    void testAsyncEndPlatformWaitsForAllRegionWrites() {
        World endWorld = mock(World.class);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);
        Location destination = new Location(endWorld, 16, 70, 16);
        java.util.List<java.util.concurrent.CompletableFuture<Void>> writes = new java.util.ArrayList<>();
        mockedUtil.when(() -> Util.getChunkAtAsync(any(Location.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(org.bukkit.Chunk.class)));
        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any()))
                    .thenAnswer(call -> {
                        var pending = new java.util.concurrent.CompletableFuture<Void>();
                        writes.add(pending);
                        return pending;
                    });
            var result = ptl.createEndPlatformAsync(destination);
            assertEquals(25, writes.size());
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            writes.forEach(write -> write.complete(null));
            org.junit.jupiter.api.Assertions.assertTrue(result.isDone());
            org.junit.jupiter.api.Assertions.assertFalse(result.isCompletedExceptionally());
        }
    }

    /**
     * Sets up a return trip from a standard (non-island) nether: the player portals from the
     * nether world back to the overworld, with nether islands and portal linking both off.
     *
     * @return the portal event, ready to be passed to
     *         {@link PlayerTeleportListener#onPlayerPortalEvent(PlayerPortalEvent)}
     */
    private PlayerPortalEvent standardNetherReturnEvent() {
        World netherWorld = mock(World.class);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        mockedUtil.when(() -> Util.getWorld(netherWorld)).thenReturn(world);
        // Standard nether: no island nethers. Portal linking is off by default in TestWorldSettings.
        when(iwm.isNetherIslands(world)).thenReturn(false);

        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(netherWorld);
        when(from.toVector()).thenReturn(new Vector(0, 0, 0));
        return new PlayerPortalEvent(mockPlayer, from, location, TeleportCause.NETHER_PORTAL, 0, false, 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     * Islands only have a spawn point if their blueprint contains a {spawn here} sign, which
     * most do not. Without one, a player returning from a standard nether must still be sent
     * to their own island's home and not to the world spawn.
     * Regression test for AOneBlock issue #549.
     */
    @Test
    void testPortalProcessFromStandardNetherNoSpawnPointUsesHome() {
        PlayerPortalEvent e = standardNetherReturnEvent();
        when(im.getIsland(world, uuid)).thenReturn(island);
        // No {spawn here} sign was pasted, so the island has no spawn point
        when(island.getSpawnPoint(Environment.NORMAL)).thenReturn(null);
        Location home = mock(Location.class);
        when(home.getWorld()).thenReturn(world);
        when(home.clone()).thenReturn(home);
        when(im.getHomeLocation(island)).thenReturn(home);

        ptl.onPlayerPortalEvent(e);

        assertSame(home, e.getTo());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     * If the island does have a spawn point, it still wins over the home location.
     */
    @Test
    void testPortalProcessFromStandardNetherSpawnPointWins() {
        PlayerPortalEvent e = standardNetherReturnEvent();
        when(im.getIsland(world, uuid)).thenReturn(island);
        Location spawnPoint = mock(Location.class);
        when(spawnPoint.getWorld()).thenReturn(world);
        when(spawnPoint.clone()).thenReturn(spawnPoint);
        when(island.getSpawnPoint(Environment.NORMAL)).thenReturn(spawnPoint);

        ptl.onPlayerPortalEvent(e);

        assertSame(spawnPoint, e.getTo());
        verify(im, never()).getHomeLocation(any(Island.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     * A player without an island still falls back to the world spawn.
     */
    @Test
    void testPortalProcessFromStandardNetherNoIslandUsesSpawn() {
        PlayerPortalEvent e = standardNetherReturnEvent();
        when(im.getIsland(world, uuid)).thenReturn(null);
        // No spawn island, but the world spawn is safe
        when(im.isSafeLocation(location)).thenReturn(true);

        ptl.onPlayerPortalEvent(e);

        assertSame(location, e.getTo());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    void testOnPlayerPortalNonPlayerEntity() {
        // Mock a non-player entity
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getType()).thenReturn(EntityType.ZOMBIE);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockEntity, location);
        ptl.onPlayerPortal(e);

        // Verify no further processing for non-player entities
        verifyNoInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    void testOnPlayerPortalAlreadyInPortal() {
        // Simulate player already in portal
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);
        ptl.onPlayerPortal(e);

        // Verify no further processing occurs
        verifyNoInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    void testOnPlayerPortalNetherPortalDisabled() {
        // Mock configuration for Nether disabled
        when(Bukkit.getAllowNether()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.NETHER_PORTAL);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);
        ptl.onPlayerPortal(e);

        // Verify PlayerPortalEvent is scheduled
        verify(Bukkit.getScheduler(), times(1)).runTaskLater(eq(plugin), any(Runnable.class), eq(40L));
    }

    /**
     * Test that when nether is disabled on the server, the scheduled task fires a PlayerPortalEvent
     * through the Bukkit event bus (so PortalListener flag checks run) instead of calling
     * portalProcess() directly.
     */
    @Test
    void testOnPlayerPortalNetherPortalDisabledCallsEvent() {
        when(Bukkit.getAllowNether()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.NETHER_PORTAL);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);
        ptl.onPlayerPortal(e);

        // Capture and execute the scheduled Runnable
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(Bukkit.getScheduler()).runTaskLater(eq(plugin), runnableCaptor.capture(), eq(40L));
        runnableCaptor.getValue().run();

        // Verify callEvent was called with a PlayerPortalEvent with NETHER_PORTAL cause
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(eventCaptor.capture());
        assertInstanceOf(PlayerPortalEvent.class, eventCaptor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, ((PlayerPortalEvent) eventCaptor.getValue()).getCause());
    }

    @Test
    void testOnPlayerPortalEndPortalDisabled() {
        // Mock configuration for End disabled
        when(Bukkit.getAllowEnd()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.END_PORTAL);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Check if the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify the event behavior indirectly by confirming the origin world was stored
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));

        // Verify callEvent was called with a PlayerPortalEvent with END_PORTAL cause
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(eventCaptor.capture());
        assertInstanceOf(PlayerPortalEvent.class, eventCaptor.getValue());
        assertEquals(TeleportCause.END_PORTAL, ((PlayerPortalEvent) eventCaptor.getValue()).getCause());
    }

    @Test
    void testOnPlayerPortalEndGatewayDisabled() {
        // Mock configuration for End disabled
        when(Bukkit.getAllowEnd()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.END_GATEWAY);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Check if the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify the event behavior indirectly by confirming the origin world was stored
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));

        // Verify callEvent was called with a PlayerPortalEvent with END_GATEWAY cause
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(eventCaptor.capture());
        assertInstanceOf(PlayerPortalEvent.class, eventCaptor.getValue());
        assertEquals(TeleportCause.END_GATEWAY, ((PlayerPortalEvent) eventCaptor.getValue()).getCause());
    }

    @Test
    void testOnPlayerPortalValidBentoBoxWorld() {
        // Mock configuration for a valid BentoBox world
        when(Bukkit.getAllowNether()).thenReturn(true);
        when(Bukkit.getAllowEnd()).thenReturn(true);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.NETHER_PORTAL);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Verify the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify teleportOrigin was updated with the correct world
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onExitPortal(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    void testOnExitPortalPlayerNotInPortal() {
        // Mock a player who is not in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, location);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that no changes occurred to inPortal or other collections
        assertFalse(ptl.getInPortal().contains(playerId));
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    @Test
    void testOnExitPortalPlayerStillInPortal() {
        // Mock a player in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);

        // Mock the destination block type as a Nether portal
        when(location.getBlock().getType()).thenReturn(Material.NETHER_PORTAL);

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, location);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that the player is still in the inPortal list
        assertTrue(ptl.getInPortal().contains(playerId));

        // Verify that no changes occurred to inTeleport or teleportOrigin
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    @Test
    void testOnExitPortalPlayerExitsPortal() {
        // Mock a player in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);
        ptl.getInTeleport().add(playerId);
        ptl.getTeleportOrigin().put(playerId, location.getWorld());

        // Mock the destination block type as something other than a Nether portal
        Location toLocation = mock(Location.class);
        when(toLocation.getBlock()).thenReturn(block);
        when(toLocation.getBlock().getType()).thenReturn(Material.AIR);

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, toLocation);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that the player was removed from inPortal, inTeleport, and teleportOrigin
        assertFalse(ptl.getInPortal().contains(playerId));
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * Respawns not caused by the end exit portal (e.g. death) are ignored.
     */
    @Test
    void testOnPlayerExitPortalNotEndPortalReason() {
        when(mockPlayer.getWorld()).thenReturn(world);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false, false, false,
                PlayerRespawnEvent.RespawnReason.DEATH);

        ptl.onPlayerExitPortal(event);

        assertSame(location, event.getRespawnLocation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * End portal respawns from non-BentoBox worlds are left to the server.
     */
    @Test
    void testOnPlayerExitPortalNotBentoBoxWorld() {
        when(mockPlayer.getWorld()).thenReturn(world);
        when(Util.getWorld(world)).thenReturn(null);
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false, false, false,
                PlayerRespawnEvent.RespawnReason.END_PORTAL);

        ptl.onPlayerExitPortal(event);

        assertSame(location, event.getRespawnLocation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * A player with an island is routed to their safe home, and any stale
     * teleport origin entry is cleaned up.
     */
    @Test
    void testOnPlayerExitPortalRoutesToSafeHome() {
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(im.getIsland(world, playerId)).thenReturn(island);
        Location home = mock(Location.class);
        when(home.getWorld()).thenReturn(world);
        when(home.clone()).thenReturn(home);
        when(im.getSafeRespawnLocation(world, playerId)).thenReturn(home);
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false, false, false,
                PlayerRespawnEvent.RespawnReason.END_PORTAL);

        ptl.onPlayerExitPortal(event);

        // Player routed to their island home, not the server spawn
        assertSame(home, event.getRespawnLocation());
        // Stale teleport origin entry cleaned up
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * When no safe home can be resolved, the respawn is anchored at the island
     * protection center and SafeSpotTeleport is scheduled.
     */
    @Test
    void testOnPlayerExitPortalNoSafeHomeSchedulesSafeSpotTeleport() {
        UUID playerId = mockPlayer.getUniqueId();
        when(mockPlayer.getWorld()).thenReturn(world);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(im.getIsland(world, playerId)).thenReturn(island);
        when(im.getSafeRespawnLocation(world, playerId)).thenReturn(null);
        Location protectionCenter = mock(Location.class);
        when(protectionCenter.getWorld()).thenReturn(world);
        when(protectionCenter.clone()).thenReturn(protectionCenter);
        when(island.getProtectionCenter()).thenReturn(protectionCenter);
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false, false, false,
                PlayerRespawnEvent.RespawnReason.END_PORTAL);

        ptl.onPlayerExitPortal(event);

        assertSame(protectionCenter, event.getRespawnLocation());
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * A player without an island is sent to the world spawn location.
     */
    @Test
    void testOnPlayerExitPortalNoIsland() {
        UUID playerId = mockPlayer.getUniqueId();
        when(mockPlayer.getWorld()).thenReturn(world);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(im.getIsland(world, playerId)).thenReturn(null);
        // No spawn island; world spawn (location) is safe
        when(im.isSafeLocation(location)).thenReturn(true);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.clone()).thenReturn(from);
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, from, false, false, false,
                PlayerRespawnEvent.RespawnReason.END_PORTAL);

        ptl.onPlayerExitPortal(event);

        // Verify that the respawn location was updated to the world spawn location
        assertSame(location, event.getRespawnLocation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     * A valid bed spawn on an island the player is a member of is honored.
     */
    @Test
    void testOnPlayerExitPortalBedSpawnHonored() {
        UUID playerId = mockPlayer.getUniqueId();
        when(mockPlayer.getWorld()).thenReturn(world);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        // World settings for the BED_ANCHOR_RESPAWN flag check (default: enabled)
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(ws.getWorldFlags()).thenReturn(new HashMap<>());
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        // Bed is on an island the player is a member of
        when(im.getProtectedIslandAt(location)).thenReturn(Optional.of(island));
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(playerId));
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, true, false, false,
                PlayerRespawnEvent.RespawnReason.END_PORTAL);

        ptl.onPlayerExitPortal(event);

        // Bed spawn honored - location unchanged and no rerouting attempted
        assertSame(location, event.getRespawnLocation());
        verify(im, never()).getSafeRespawnLocation(any(), any());
    }

    @Test
    void testPasteOnceRunsOnePastePerIslandDimension() {
        Island pasteIsland = mock(Island.class);
        when(pasteIsland.getUniqueId()).thenReturn("paste-once-island");
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Supplier<java.util.concurrent.CompletableFuture<Void>> action = () -> {
            calls.incrementAndGet();
            return paste;
        };

        var first = ptl.pasteOnce(pasteIsland, Environment.NETHER, action);
        var second = ptl.pasteOnce(pasteIsland, Environment.NETHER, action);
        assertSame(first, second);
        assertEquals(1, calls.get());

        paste.complete(null);
        // A finished paste is not cached, so a later portal trip may paste again.
        ptl.pasteOnce(pasteIsland, Environment.NETHER, action);
        assertEquals(2, calls.get());
    }

    /**
     * On Paper the server continues the transfer unless the event is cancelled. A missing
     * Nether island therefore has to be pasted first, exactly like the Folia path.
     */
    @Test
    void testPaperPastesMissingNetherIslandBeforeTransfer() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(iwm.getNetherWorld(world)).thenReturn(nether);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, Environment.NETHER)).thenReturn(true);
        when(ptl.isPastingMissingIslands(world)).thenReturn(true);

        Island target = mock(Island.class);
        when(target.getWorld()).thenReturn(world);
        when(target.hasNetherIsland()).thenReturn(false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(target));

        BlueprintsManager bpm = mock(BlueprintsManager.class);
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        GameModeAddon addon = mock(GameModeAddon.class);
        WorldSettings addonSettings = mock(WorldSettings.class);
        when(addonSettings.isMakeNetherPortals()).thenReturn(false);
        when(addon.getWorldSettings()).thenReturn(addonSettings);
        when(iwm.getAddon(world)).thenReturn(Optional.of(addon));
        BlueprintBundle bundle = mock(BlueprintBundle.class);
        when(bpm.getDefaultBlueprintBundle(addon)).thenReturn(bundle);
        when(bundle.getBlueprint(Environment.NETHER)).thenReturn("nether-island");
        when(bpm.getBlueprints(addon)).thenReturn(Map.of("nether-island", mock(Blueprint.class)));

        PlayerTeleportListener listener = org.mockito.Mockito.spy(ptl);
        // The paste itself is covered elsewhere; a pending future keeps the transfer waiting.
        org.mockito.Mockito.doReturn(new java.util.concurrent.CompletableFuture<Void>())
                .when(listener).pasteOnce(any(), any(), any());

        Location from = new Location(world, 10, 80, 10);
        PlayerPortalEvent event = new PlayerPortalEvent(mockPlayer, from, from,
                TeleportCause.NETHER_PORTAL, 0, false, 0);
        listener.onPlayerPortalEvent(event);

        assertTrue(event.isCancelled());
        verify(listener).pasteOnce(eq(target), eq(Environment.NETHER), any());
    }

}
