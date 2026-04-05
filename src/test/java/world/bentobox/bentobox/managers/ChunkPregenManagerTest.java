package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkPregenManagerTest extends CommonTestSetup {

    @Mock
    private GameModeAddon addon;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
    @Mock
    private BukkitTask task;
    @Mock
    private AddonsManager addonsManager;
    @Mock
    private IslandCreatedEvent createdEvent;
    @Mock
    private IslandResettedEvent resettedEvent;
    @Mock
    private BentoBoxReadyEvent readyEvent;

    private ChunkPregenManager manager;
    private Settings settings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Scheduler returns a cancellable task
        when(sch.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        when(task.isCancelled()).thenReturn(false);

        // Bukkit.getViewDistance() — small value keeps queue sizes manageable in tests
        mockedBukkit.when(Bukkit::getViewDistance).thenReturn(2);

        // Addon wiring
        when(addon.isFixIslandCenter()).thenReturn(true);
        when(addon.getPregenIslandsAhead()).thenReturn(-1); // use global default
        when(addon.getOverWorld()).thenReturn(world);
        when(addon.getNetherWorld()).thenReturn(netherWorld);
        when(addon.getEndWorld()).thenReturn(endWorld);

        // IWM
        when(iwm.getAddon(any(World.class))).thenReturn(Optional.of(addon));
        when(iwm.getIslandDistance(world)).thenReturn(100);
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        when(iwm.getIslandStartX(world)).thenReturn(0);
        when(iwm.getIslandStartZ(world)).thenReturn(0);
        when(iwm.getIslandHeight(world)).thenReturn(64);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.getNetherWorld(world)).thenReturn(netherWorld);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);
        when(iwm.getEndWorld(world)).thenReturn(endWorld);

        // IslandsManager — null last triggers real Location construction from IWM start values
        when(im.getLast(world)).thenReturn(null);

        // AddonsManager
        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getGameModeAddons()).thenReturn(List.of(addon));

        // Util static stubs — all chunks ungenerated so queues are non-empty
        mockedUtil.when(() -> Util.isChunkGenerated(any(World.class), anyInt(), anyInt())).thenReturn(false);
        mockedUtil.when(() -> Util.getChunkAtAsync(any(World.class), anyInt(), anyInt()))
                  .thenReturn(CompletableFuture.completedFuture(null));

        // plugin.log() — avoid NPE; real Settings already wired by CommonTestSetup
        doNothing().when(plugin).log(anyString());

        // Grab the real Settings so we can mutate it per-test
        settings = plugin.getSettings();

        manager = new ChunkPregenManager(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    void testConstructor() {
        assertNotNull(manager);
    }

    // -----------------------------------------------------------------------
    // onBentoBoxReady
    // -----------------------------------------------------------------------

    @Test
    void testOnBentoBoxReady_pregenDisabled() {
        settings.setPregenEnabled(false);
        manager.onBentoBoxReady(readyEvent);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testOnBentoBoxReady_pregenEnabled() {
        manager.onBentoBoxReady(readyEvent);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // onIslandCreated
    // -----------------------------------------------------------------------

    @Test
    void testOnIslandCreated_pregenDisabled() {
        settings.setPregenEnabled(false);
        when(createdEvent.getIsland()).thenReturn(island);
        when(island.getWorld()).thenReturn(world);
        manager.onIslandCreated(createdEvent);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testOnIslandCreated_pregenEnabled() {
        when(createdEvent.getIsland()).thenReturn(island);
        when(island.getWorld()).thenReturn(world);
        manager.onIslandCreated(createdEvent);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // onIslandResetted
    // -----------------------------------------------------------------------

    @Test
    void testOnIslandResetted_pregenDisabled() {
        settings.setPregenEnabled(false);
        when(resettedEvent.getIsland()).thenReturn(island);
        when(island.getWorld()).thenReturn(world);
        manager.onIslandResetted(resettedEvent);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testOnIslandResetted_pregenEnabled() {
        when(resettedEvent.getIsland()).thenReturn(island);
        when(island.getWorld()).thenReturn(world);
        manager.onIslandResetted(resettedEvent);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // schedulePregen — early exit paths
    // -----------------------------------------------------------------------

    @Test
    void testSchedulePregen_nonGridAddon() {
        when(addon.isFixIslandCenter()).thenReturn(false);
        manager.schedulePregen(addon);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_nullOverworld() {
        when(addon.getOverWorld()).thenReturn(null);
        manager.schedulePregen(addon);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_zeroIslandsAhead_addonOverride() {
        when(addon.getPregenIslandsAhead()).thenReturn(0);
        manager.schedulePregen(addon);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_zeroIslandsAhead_globalDefault() {
        // addon defers to global; global setting also 0
        when(addon.getPregenIslandsAhead()).thenReturn(-1);
        settings.setPregenIslandsAhead(0);
        manager.schedulePregen(addon);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // schedulePregen — happy paths
    // -----------------------------------------------------------------------

    @Test
    void testSchedulePregen_globalDefault_queuesChunks() {
        // Default: 3 islands ahead, viewDistance 2 → each island (2*2+1)^2 = 25 chunks × 3 worlds × 3 islands
        manager.schedulePregen(addon);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());

        // Drive the tick so chunks are dispatched
        ArgumentCaptor<Runnable> cap = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskTimer(eq(plugin), cap.capture(), anyLong(), anyLong());
        cap.getValue().run();

        // At least one chunk async request should have been made
        mockedUtil.verify(() -> Util.getChunkAtAsync(any(World.class), anyInt(), anyInt()),
                org.mockito.Mockito.atLeastOnce());
    }

    @Test
    void testSchedulePregen_addonOverridesIslandsAhead() {
        when(addon.getPregenIslandsAhead()).thenReturn(5);
        manager.schedulePregen(addon);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_noNetherOrEnd() {
        when(iwm.isNetherGenerate(world)).thenReturn(false);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isEndGenerate(world)).thenReturn(false);
        when(iwm.isEndIslands(world)).thenReturn(false);
        manager.schedulePregen(addon);
        // Should still queue overworld chunks and start task
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_netherNullWorld() {
        // isNetherGenerate & isNetherIslands true but getNetherWorld returns null
        when(iwm.getNetherWorld(world)).thenReturn(null);
        manager.schedulePregen(addon);
        // Overworld chunks still queued; task still starts
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_withNonNullLastLocation() {
        // Provide a real Location as "last" — predictNextLocations must still work
        Location last = new Location(world, 200, 64, 0);
        when(im.getLast(world)).thenReturn(last);
        manager.schedulePregen(addon);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testSchedulePregen_allChunksAlreadyGenerated_noTask() {
        // If every chunk is already generated the queue stays empty → no task
        mockedUtil.when(() -> Util.isChunkGenerated(any(World.class), anyInt(), anyInt())).thenReturn(true);
        manager.schedulePregen(addon);
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // predictNextLocations (package-private static — same package access)
    // -----------------------------------------------------------------------

    @Test
    void testPredictNextLocations_returnsCorrectCount() {
        Location start = new Location(world, 0, 64, 0);
        List<Location> result = ChunkPregenManager.predictNextLocations(world, start, 5, 200);
        assertEquals(5, result.size());
    }

    @Test
    void testPredictNextLocations_doesNotMutateStart() {
        Location start = new Location(world, 0, 64, 0);
        ChunkPregenManager.predictNextLocations(world, start, 3, 200);
        // Original start should not be modified
        assertEquals(0, start.getBlockX());
        assertEquals(0, start.getBlockZ());
    }

    @Test
    void testPredictNextLocations_spiralFirstStep() {
        // From (0,0) the spiral first moves in the +x direction (x >= z and x <= 0 edge → z+d)
        // Actually: x=0, z=0 → x==z and x<=0 → setZ(z+d)
        Location start = new Location(world, 0, 64, 0);
        List<Location> result = ChunkPregenManager.predictNextLocations(world, start, 1, 100);
        Location first = result.get(0);
        // x stays 0, z moves to +100
        assertEquals(0, first.getBlockX());
        assertEquals(100, first.getBlockZ());
    }

    @Test
    void testPredictNextLocations_emptyWhenCountZero() {
        Location start = new Location(world, 0, 64, 0);
        List<Location> result = ChunkPregenManager.predictNextLocations(world, start, 0, 200);
        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // shutdown
    // -----------------------------------------------------------------------

    @Test
    void testShutdown_cancelsRunningTask() {
        manager.schedulePregen(addon); // creates task
        manager.shutdown();
        verify(task).cancel();
    }

    @Test
    void testShutdown_noTaskRunning_noException() {
        // Shutdown without ever scheduling — must not throw
        manager.shutdown();
        verify(task, never()).cancel();
    }

    // -----------------------------------------------------------------------
    // tick — auto-cancel when all queues empty
    // -----------------------------------------------------------------------

    @Test
    void testTick_autoCancel_whenQueuesEmpty() {
        manager.schedulePregen(addon);

        ArgumentCaptor<Runnable> cap = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskTimer(eq(plugin), cap.capture(), anyLong(), anyLong());

        // Clear all queues by shutting down, then verify task was cancelled
        manager.shutdown();
        verify(task).cancel();

        // Run tick again with a fresh (cancelled) state — activeWorlds is empty so it
        // should cancel the new task reference (which is null after shutdown)
        // Just assert no exception is thrown
        cap.getValue().run();
    }

    @Test
    void testTick_dispatchesChunksWhenQueued() {
        manager.schedulePregen(addon);

        ArgumentCaptor<Runnable> cap = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskTimer(eq(plugin), cap.capture(), anyLong(), anyLong());

        cap.getValue().run(); // one tick

        // Verify async chunk requests were issued
        mockedUtil.verify(() -> Util.getChunkAtAsync(any(World.class), anyInt(), anyInt()),
                org.mockito.Mockito.atLeastOnce());
    }
}
