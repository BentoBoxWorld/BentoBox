package world.bentobox.bentobox.util.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport.Builder;

/**
 * Tests for {@link ClosestSafeSpotTeleport}.
 *
 * <p>ClosestSafeSpotTeleport performs an async safe-spot search centred on a given
 * {@link Location} and teleports an entity to the nearest suitable position. The
 * algorithm works in three phases:
 * <ol>
 *   <li><b>Fast-path:</b> if the starting location is already safe (and this is not a
 *       portal search) the entity is teleported immediately.</li>
 *   <li><b>Chunk scan:</b> chunks are loaded one per scheduler tick, candidates are
 *       collected into a distance-ordered priority queue.</li>
 *   <li><b>Finalisation:</b> the best candidate (or a portal candidate, or a fallback
 *       block creation) is used for the actual teleport.</li>
 * </ol>
 *
 * <p>The constructor immediately schedules an async chunk load via
 * {@link Util#getChunkAtAsync(Location)}. To keep individual tests synchronous and
 * deterministic, every test stubs that method with a {@link CompletableFuture} that
 * never completes, preventing the automatic {@link ClosestSafeSpotTeleport#checkLocation()}
 * call. Tests that need {@code checkLocation()} to run invoke it explicitly.
 *
 * <p>Private fields are injected via the {@link #setField(Object, String, Object)} helper
 * so that internal state (block queue, task reference, flags) can be primed for
 * fine-grained unit tests without going through the full async pipeline.
 */
class ClosestSafeSpotTeleportTest extends CommonTestSetup {

    // -----------------------------------------------------------------------
    // Extra mocks
    // -----------------------------------------------------------------------

    @Mock
    private BukkitTask task;
    @Mock
    private ChunkSnapshot snapshot;
    @Mock
    private Block blockDown;
    @Mock
    private Block blockUp;
    @Mock
    private Block blockUpUp;
    @Mock
    private Block highestBlock;

    /**
     * A future that never completes. Returned by all {@code getChunkAtAsync} stubs so that
     * the constructor-triggered chunk load does not automatically call
     * {@link ClosestSafeSpotTeleport#checkLocation()}.
     */
    private CompletableFuture<Chunk> pendingFuture;

    /** Instance under test; built lazily per test via {@link #buildTeleport(boolean)}. */
    private ClosestSafeSpotTeleport teleport;

    // -----------------------------------------------------------------------
    // Setup / Teardown
    // -----------------------------------------------------------------------

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        pendingFuture = new CompletableFuture<>();

        // Prevent the constructor-triggered chunk load from calling checkLocation().
        mockedUtil.when(() -> Util.getChunkAtAsync(any(Location.class)))
                .thenReturn(pendingFuture);
        mockedUtil.when(() -> Util.getChunkAtAsync(any(World.class), anyInt(), anyInt()))
                .thenReturn(pendingFuture);

        // teleportAsync always succeeds; thenRun() fires synchronously for testing.
        mockedUtil.when(() -> Util.teleportAsync(any(), any(Location.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Scheduler – runTask executes the runnable synchronously so tests stay sync.
        when(sch.runTask(eq(plugin), any(Runnable.class))).thenAnswer(inv -> {
            inv.getArgument(1, Runnable.class).run();
            return task;
        });
        when(sch.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(task);

        // World heights and environment.
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Island manager – getIslandsManager() delegates to getIslands() on the real class
        // but the mock records them as separate method calls.
        when(plugin.getIslandsManager()).thenReturn(im);

        // Island – large protection bounding box and all coordinates in island space.
        BoundingBox bbox = new BoundingBox(-200, -64, -200, 200, 320, 200);
        when(island.getProtectionBoundingBox()).thenReturn(bbox);
        when(island.inIslandSpace(any(Pair.class))).thenReturn(true);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));

        // Block stubs used by makeAndTeleport().
        Block block = mock(Block.class);
        when(location.getBlock()).thenReturn(block);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(blockDown);
        when(block.getRelative(BlockFace.UP)).thenReturn(blockUp);
        when(blockUp.getRelative(BlockFace.UP)).thenReturn(blockUpUp);
        when(location.add(any(Vector.class))).thenReturn(location);

        // Highest block for returnAndTeleport() fall-through path.
        when(world.getHighestBlockAt(any(Location.class))).thenReturn(highestBlock);
        when(highestBlock.getType()).thenReturn(Material.AIR); // not solid → skip path
        when(highestBlock.getLocation()).thenReturn(location);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------------
    // Reflection helpers
    // -----------------------------------------------------------------------

    /**
     * Sets a private instance field on {@code target} via reflection.
     *
     * @param target    the object whose field will be written
     * @param fieldName the simple name of the declared field
     * @param value     the new value
     * @throws Exception on any reflection error
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Reads a private instance field from {@code target} via reflection.
     *
     * @param <T>       the expected type
     * @param target    the object whose field will be read
     * @param fieldName the simple name of the declared field
     * @return the current value of the field, cast to {@code T}
     * @throws Exception on any reflection error
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    // -----------------------------------------------------------------------
    // Builder helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link ClosestSafeSpotTeleport} using {@link #mockPlayer} and
     * {@link #location}. {@link ClosestSafeSpotTeleport#checkLocation()} is NOT called
     * automatically because the constructor's chunk-load future never completes.
     *
     * @param portal {@code true} to enable portal-search mode
     * @return a freshly constructed instance
     */
    private ClosestSafeSpotTeleport buildTeleport(boolean portal) {
        Builder b = ClosestSafeSpotTeleport.builder(plugin)
                .entity(mockPlayer)
                .location(location);
        if (portal) {
            b.portal();
        }
        return b.build();
    }

    // -----------------------------------------------------------------------
    // Builder tests
    // -----------------------------------------------------------------------

    /**
     * When no entity is supplied, {@link Builder#build()} returns {@code null}, logs an
     * error, and completes the result future with {@code null}.
     */
    @Test
    void testBuilder_nullEntity_returnsNull() {
        Builder b = ClosestSafeSpotTeleport.builder(plugin).location(location);

        assertNull(b.build());
        assertNull(b.getResult().join());
        verify(plugin).logError(anyString());
    }

    /**
     * When no location is supplied, {@link Builder#build()} returns {@code null}.
     */
    @Test
    void testBuilder_nullLocation_returnsNull() {
        Builder b = ClosestSafeSpotTeleport.builder(plugin).entity(mockPlayer);

        assertNull(b.build());
        assertNull(b.getResult().join());
        verify(plugin).logError(anyString());
    }

    /**
     * When the location's world is {@code null}, {@link Builder#build()} returns
     * {@code null}.
     */
    @Test
    void testBuilder_nullWorld_returnsNull() {
        when(location.getWorld()).thenReturn(null);
        Builder b = ClosestSafeSpotTeleport.builder(plugin)
                .entity(mockPlayer).location(location);

        assertNull(b.build());
        assertNull(b.getResult().join());
        verify(plugin).logError(anyString());
    }

    /**
     * A fully configured builder produces a non-null instance.
     */
    @Test
    void testBuilder_validBuild_returnsInstance() {
        assertNotNull(buildTeleport(false));
    }

    /**
     * {@link Builder#portal()} correctly sets the portal flag and the getter reflects it.
     */
    @Test
    void testBuilder_portalFlag_isSet() {
        assertTrue(ClosestSafeSpotTeleport.builder(plugin).portal().isPortal());
    }

    /**
     * {@link Builder#successRunnable(Runnable)} stores the runnable and the getter returns
     * the same instance.
     */
    @Test
    void testBuilder_successRunnable_isStored() {
        Runnable r = mock(Runnable.class);
        Builder b = ClosestSafeSpotTeleport.builder(plugin).successRunnable(r);

        assertSame(r, b.getSuccessRunnable());
    }

    // -----------------------------------------------------------------------
    // checkLocation() tests
    // -----------------------------------------------------------------------

    /**
     * When the target location is safe and portal mode is off, {@code checkLocation()}
     * teleports the entity immediately and does not start the recurring chunk-scan timer.
     */
    @Test
    void testCheckLocation_safeNotPortal_teleportsImmediatelyWithoutTimer() {
        when(im.isSafeLocation(location)).thenReturn(true);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        verify(sch).runTask(eq(plugin), any(Runnable.class));
        verify(sch, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
    }

    /**
     * When the target location is safe but portal mode is on, the fast-path is skipped and
     * the full chunk scan is started.
     */
    @Test
    void testCheckLocation_safeButPortal_startsScan() {
        when(im.isSafeLocation(location)).thenReturn(true);
        teleport = buildTeleport(true);
        teleport.checkLocation();

        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    /**
     * When the target location is unsafe, the chunk scan timer is started regardless of
     * portal mode.
     */
    @Test
    void testCheckLocation_unsafeLocation_startsScan() {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    // -----------------------------------------------------------------------
    // getChunksToScan() tests
    // -----------------------------------------------------------------------

    /**
     * {@code getChunksToScan()} returns a non-empty list of chunk coordinates around the
     * starting location.
     */
    @Test
    void testGetChunksToScan_returnsNonEmptyList() {
        teleport = buildTeleport(false);

        assertFalse(teleport.getChunksToScan().isEmpty());
    }

    /**
     * The list returned by {@code getChunksToScan()} contains no duplicate chunk
     * coordinates.
     */
    @Test
    void testGetChunksToScan_noDuplicates() {
        teleport = buildTeleport(false);
        List<Pair<Integer, Integer>> chunks = teleport.getChunksToScan();
        long unique = chunks.stream().distinct().count();

        assertEquals(unique, chunks.size(), "Chunk list must not contain duplicates");
    }

    // -----------------------------------------------------------------------
    // addChunk() tests
    // -----------------------------------------------------------------------

    /**
     * A new chunk whose block coordinate is inside island space is appended to the list.
     */
    @Test
    void testAddChunk_newChunkInIslandSpace_isAdded() {
        teleport = buildTeleport(false);
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        Pair<Integer, Integer> blockCoord = new Pair<>(0, 0);
        Pair<Integer, Integer> chunkCoord = new Pair<>(0, 0);
        when(island.inIslandSpace(blockCoord)).thenReturn(true);

        teleport.addChunk(list, blockCoord, chunkCoord);

        assertEquals(1, list.size());
        assertEquals(chunkCoord, list.get(0));
    }

    /**
     * A chunk coordinate that already exists in the list is not added a second time.
     */
    @Test
    void testAddChunk_duplicate_notAdded() {
        teleport = buildTeleport(false);
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        Pair<Integer, Integer> coord = new Pair<>(0, 0);
        list.add(coord);

        teleport.addChunk(list, coord, coord);

        assertEquals(1, list.size());
    }

    /**
     * A chunk is not added when the island's {@code inIslandSpace()} returns {@code false}.
     */
    @Test
    void testAddChunk_outsideIslandSpace_notAdded() {
        teleport = buildTeleport(false);
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        Pair<Integer, Integer> blockCoord = new Pair<>(999, 999);
        Pair<Integer, Integer> chunkCoord = new Pair<>(62, 62);
        when(island.inIslandSpace(blockCoord)).thenReturn(false);

        teleport.addChunk(list, blockCoord, chunkCoord);

        assertTrue(list.isEmpty());
    }

    /**
     * When no island is present at the location ({@code Optional.empty()}), any new chunk
     * coordinate is added unconditionally.
     */
    @Test
    void testAddChunk_noIsland_addsChunkUnconditionally() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        teleport = buildTeleport(false);
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        Pair<Integer, Integer> blockCoord = new Pair<>(32, 32);
        Pair<Integer, Integer> chunkCoord = new Pair<>(2, 2);

        teleport.addChunk(list, blockCoord, chunkCoord);

        assertEquals(1, list.size());
    }

    // -----------------------------------------------------------------------
    // checkPosition() tests
    // -----------------------------------------------------------------------

    /**
     * In non-portal mode the first position always triggers teleportation and the method
     * returns {@code true}.
     */
    @Test
    void testCheckPosition_notPortal_teleportsAndReturnsTrue() {
        teleport = buildTeleport(false);
        ClosestSafeSpotTeleport.PositionData pos = new ClosestSafeSpotTeleport.PositionData(
                new Vector(1, 64, 1), Material.STONE, Material.AIR, Material.AIR, 3.0);

        assertTrue(teleport.checkPosition(pos));
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * In portal mode, when {@code spaceOne} is {@link Material#NETHER_PORTAL}, the entity
     * is teleported to that portal block and the method returns {@code true}.
     */
    @Test
    void testCheckPosition_portalMode_netherPortalInSpaceOne_returnsTrue() {
        teleport = buildTeleport(true);
        ClosestSafeSpotTeleport.PositionData pos = new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.OBSIDIAN, Material.NETHER_PORTAL, Material.AIR, 1.0);

        assertTrue(teleport.checkPosition(pos));
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * In portal mode, when {@code spaceTwo} is {@link Material#NETHER_PORTAL}, the entity
     * is teleported to the portal block and the method returns {@code true}.
     */
    @Test
    void testCheckPosition_portalMode_netherPortalInSpaceTwo_returnsTrue() {
        teleport = buildTeleport(true);
        ClosestSafeSpotTeleport.PositionData pos = new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.OBSIDIAN, Material.AIR, Material.NETHER_PORTAL, 1.0);

        assertTrue(teleport.checkPosition(pos));
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * In portal mode, when neither space contains {@link Material#NETHER_PORTAL}, the first
     * position is stored as {@code noPortalPosition} and the method returns {@code false} so
     * the scan continues looking for a real portal.
     */
    @Test
    void testCheckPosition_portalMode_noPortal_storesNoPortalPositionAndReturnsFalse()
            throws Exception {
        teleport = buildTeleport(true);
        ClosestSafeSpotTeleport.PositionData pos = new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.STONE, Material.AIR, Material.AIR, 1.0);

        assertFalse(teleport.checkPosition(pos));
        assertNotNull(getField(teleport, "noPortalPosition"));
    }

    /**
     * A second non-portal position must not overwrite the already-stored
     * {@code noPortalPosition}.
     */
    @Test
    void testCheckPosition_portalMode_secondNonPortal_doesNotOverwriteNoPortalPosition()
            throws Exception {
        teleport = buildTeleport(true);
        Location firstFallback = mock(Location.class);
        setField(teleport, "noPortalPosition", firstFallback);

        ClosestSafeSpotTeleport.PositionData pos = new ClosestSafeSpotTeleport.PositionData(
                new Vector(5, 64, 5), Material.STONE, Material.AIR, Material.AIR, 10.0);
        teleport.checkPosition(pos);

        assertSame(firstFallback, getField(teleport, "noPortalPosition"));
    }

    // -----------------------------------------------------------------------
    // scanBlockQueue() tests
    // -----------------------------------------------------------------------

    /**
     * An empty block queue means no safe location was found; {@code scanBlockQueue()}
     * returns {@code false}.
     */
    @Test
    void testScanBlockQueue_emptyQueue_returnsFalse() throws Exception {
        teleport = buildTeleport(false);
        setField(teleport, "blockQueue", new PriorityQueue<>());

        assertFalse(teleport.scanBlockQueue());
    }

    /**
     * A queue containing one safe position causes {@code scanBlockQueue()} to schedule a
     * teleport and return {@code true}.
     */
    @Test
    void testScanBlockQueue_onePosition_teleportsAndReturnsTrue() throws Exception {
        teleport = buildTeleport(false);
        PriorityQueue<ClosestSafeSpotTeleport.PositionData> queue = new PriorityQueue<>(
                Comparator.comparingDouble(ClosestSafeSpotTeleport.PositionData::distance));
        queue.add(new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.STONE, Material.AIR, Material.AIR, 1.0));
        setField(teleport, "blockQueue", queue);

        assertTrue(teleport.scanBlockQueue());
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    // -----------------------------------------------------------------------
    // scanAndPopulateBlockQueue() tests
    // -----------------------------------------------------------------------

    /**
     * Positions inside the bounding box that pass {@code checkIfSafe} are added to the
     * block queue.
     */
    @Test
    void testScanAndPopulateBlockQueue_safePositionInBounds_addedToQueue() throws Exception {
        teleport = buildTeleport(false);

        // Bounding box that covers the entire chunk (0-15) at y 0 ± range.
        setField(teleport, "boundingBox", new BoundingBox(-1, -6, -1, 16, 6, 16));
        setField(teleport, "range", 5);
        PriorityQueue<ClosestSafeSpotTeleport.PositionData> queue = new PriorityQueue<>(1,
                Comparator.comparingDouble(ClosestSafeSpotTeleport.PositionData::distance));
        setField(teleport, "blockQueue", queue);

        when(snapshot.getX()).thenReturn(0);
        when(snapshot.getZ()).thenReturn(0);
        when(snapshot.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        when(im.checkIfSafe(any(World.class), any(), any(), any())).thenReturn(true);

        teleport.scanAndPopulateBlockQueue(snapshot);

        assertFalse(queue.isEmpty(), "Safe positions within bounds must be added to the queue");
    }

    /**
     * Positions whose coordinates fall outside the bounding box are silently skipped.
     */
    @Test
    void testScanAndPopulateBlockQueue_outsideBoundingBox_notAdded() throws Exception {
        teleport = buildTeleport(false);

        // Bounding box far from chunk (0,0).
        setField(teleport, "boundingBox", new BoundingBox(500, 64, 500, 600, 128, 600));
        setField(teleport, "range", 5);
        PriorityQueue<ClosestSafeSpotTeleport.PositionData> queue = new PriorityQueue<>(1,
                Comparator.comparingDouble(ClosestSafeSpotTeleport.PositionData::distance));
        setField(teleport, "blockQueue", queue);

        when(snapshot.getX()).thenReturn(0);
        when(snapshot.getZ()).thenReturn(0);

        teleport.scanAndPopulateBlockQueue(snapshot);

        assertTrue(queue.isEmpty(), "Positions outside the bounding box must not be added");
    }

    // -----------------------------------------------------------------------
    // makeAndTeleport() tests
    // -----------------------------------------------------------------------

    /**
     * In the NORMAL environment, {@code makeAndTeleport()} places COBBLESTONE as the base
     * and ceiling blocks and AIR in the two spaces between them.
     */
    @Test
    void testMakeAndTeleport_cobblestone_setsExpectedBlocks() {
        teleport = buildTeleport(false);
        teleport.makeAndTeleport(Material.COBBLESTONE);

        verify(blockDown).setType(Material.COBBLESTONE, false);
        verify(blockUpUp).setType(Material.COBBLESTONE, false);
        verify(location.getBlock()).setType(Material.AIR, false);
        verify(blockUp).setType(Material.AIR, false);
    }

    /**
     * {@code makeAndTeleport()} with NETHERRACK places that material at the base and
     * ceiling positions.
     */
    @Test
    void testMakeAndTeleport_netherrack_setsExpectedBlocks() {
        teleport = buildTeleport(false);
        teleport.makeAndTeleport(Material.NETHERRACK);

        verify(blockDown).setType(Material.NETHERRACK, false);
        verify(blockUpUp).setType(Material.NETHERRACK, false);
    }

    /**
     * {@code makeAndTeleport()} with END_STONE places that material at the base and ceiling
     * positions.
     */
    @Test
    void testMakeAndTeleport_endStone_setsExpectedBlocks() {
        teleport = buildTeleport(false);
        teleport.makeAndTeleport(Material.END_STONE);

        verify(blockDown).setType(Material.END_STONE, false);
        verify(blockUpUp).setType(Material.END_STONE, false);
    }

    // -----------------------------------------------------------------------
    // gatherChunks() tests
    // -----------------------------------------------------------------------

    /**
     * When the {@code checking} flag is already {@code true}, {@code gatherChunks()} returns
     * early without cancelling the task or loading any chunk.
     */
    @Test
    void testGatherChunks_alreadyChecking_earlyReturn() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        setField(teleport, "checking", new AtomicBoolean(true));
        teleport.gatherChunks();

        verify(task, never()).cancel();
    }

    /**
     * When the chunk iterator is exhausted, {@code gatherChunks()} calls
     * {@link ClosestSafeSpotTeleport#finishTask()}, which cancels the recurring timer.
     */
    @Test
    void testGatherChunks_noMoreChunks_finishesTask() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        setField(teleport, "cancelIfFail", true);
        setField(teleport, "chunksToScanIterator",
                new ArrayList<Pair<Integer, Integer>>().iterator());

        teleport.gatherChunks();

        verify(task).cancel();
    }

    /**
     * When the block queue already contains a position within 5 units of the origin and
     * portal mode is off, {@code gatherChunks()} finishes the task early rather than loading
     * more chunks.
     */
    @Test
    void testGatherChunks_closePositionInQueue_notPortal_finishesEarly() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        setField(teleport, "cancelIfFail", true);
        PriorityQueue<ClosestSafeSpotTeleport.PositionData> queue = new PriorityQueue<>(
                Comparator.comparingDouble(ClosestSafeSpotTeleport.PositionData::distance));
        queue.add(new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.STONE, Material.AIR, Material.AIR, 2.0));
        setField(teleport, "blockQueue", queue);

        teleport.gatherChunks();

        verify(task).cancel();
    }

    // -----------------------------------------------------------------------
    // finishTask() tests
    // -----------------------------------------------------------------------

    /**
     * {@code finishTask()} always cancels the recurring Bukkit timer, regardless of the
     * state of the block queue.
     */
    @Test
    void testFinishTask_alwaysCancelsTask() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        // Use cancelIfFail=true to keep the execution path short and predictable.
        setField(teleport, "cancelIfFail", true);
        setField(teleport, "blockQueue", new PriorityQueue<>());

        teleport.finishTask();

        verify(task).cancel();
    }

    /**
     * When {@code finishTask()} finds a safe position in the block queue, it schedules the
     * teleport and the result future completes with {@code true}.
     */
    @Test
    void testFinishTask_safePositionInQueue_completesTrue() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        PriorityQueue<ClosestSafeSpotTeleport.PositionData> queue = new PriorityQueue<>(
                Comparator.comparingDouble(ClosestSafeSpotTeleport.PositionData::distance));
        queue.add(new ClosestSafeSpotTeleport.PositionData(
                new Vector(0, 64, 0), Material.STONE, Material.AIR, Material.AIR, 1.0));
        setField(teleport, "blockQueue", queue);

        CompletableFuture<Boolean> result = getField(teleport, "result");
        teleport.finishTask();

        assertTrue(result.join());
    }

    /**
     * In portal mode with an empty queue but a stored {@code noPortalPosition}, the entity
     * is teleported to that fallback location.
     */
    @Test
    void testFinishTask_portalMode_noPortalFound_teleportsToFallback() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(true);
        teleport.checkLocation();

        setField(teleport, "blockQueue", new PriorityQueue<>());
        setField(teleport, "noPortalPosition", mock(Location.class));

        teleport.finishTask();

        // teleportEntity schedules an asyncTeleport on the main thread.
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * When no position is found, no portal is set, and the entity is a player,
     * {@code finishTask()} schedules {@code returnAndTeleport()} on the main thread, and the
     * result future completes with {@code false} when {@code cancelIfFail} is {@code true}.
     */
    @Test
    void testFinishTask_noPosition_player_cancelIfFail_completesFalse() throws Exception {
        when(im.isSafeLocation(location)).thenReturn(false);
        teleport = buildTeleport(false);
        teleport.checkLocation();

        setField(teleport, "cancelIfFail", true);
        setField(teleport, "blockQueue", new PriorityQueue<>());

        CompletableFuture<Boolean> result = getField(teleport, "result");
        teleport.finishTask();

        assertFalse(result.join());
    }

    // -----------------------------------------------------------------------
    // asyncTeleport() / successRunnable tests
    // -----------------------------------------------------------------------

    /**
     * When a {@code successRunnable} is provided, it is dispatched on the main thread after
     * the teleportation future completes.
     */
    @Test
    void testAsyncTeleport_withSuccessRunnable_runsRunnable() {
        Runnable success = mock(Runnable.class);
        ClosestSafeSpotTeleport t = ClosestSafeSpotTeleport.builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .successRunnable(success)
                .build();

        t.asyncTeleport(location);

        verify(success).run();
    }

    /**
     * When no {@code successRunnable} is provided, {@code asyncTeleport()} still completes
     * the result future with {@code true}.
     */
    @Test
    void testAsyncTeleport_noSuccessRunnable_completesTrue() throws Exception {
        teleport = buildTeleport(false);
        CompletableFuture<Boolean> result = getField(teleport, "result");

        teleport.asyncTeleport(location);

        assertTrue(result.join());
    }
}
