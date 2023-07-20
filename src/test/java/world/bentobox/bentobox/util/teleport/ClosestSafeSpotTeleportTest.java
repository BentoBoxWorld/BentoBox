package world.bentobox.bentobox.util.teleport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport.Builder;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport.PositionData;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class, Bukkit.class})
public class ClosestSafeSpotTeleportTest {

    // Class under test
    private ClosestSafeSpotTeleport csst;

    @Mock
    private BentoBox plugin;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Player entity;

    @Mock
    private Runnable runnable;
    @Mock
    private Runnable failRunnable;
    @Mock
    private CompletableFuture<Boolean> result;
    @Mock
    private @NonNull CompletableFuture<Chunk> cfChunk;
    @Mock
    private IslandsManager im;
    @Mock
    private BukkitScheduler scheduler;

    private Island island;
    @Mock
    private IslandWorldManager iwm;

    @Mock
    private BukkitTask task;
    @Mock
    private ChunkSnapshot chunkSnapshot;
    @Mock
    private Block block;

    private Builder builder;
    /**
     */
    @Before
    public void setUp() throws Exception {
        // Setup instance
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // IWM
        when(iwm.getIslandProtectionRange(any())).thenReturn(100);
        when(iwm.getIslandDistance(any())).thenReturn(400);
        when(plugin.getIWM()).thenReturn(iwm);
        when(plugin.getIslandsManager()).thenReturn(im);
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Mock static Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getChunkAtAsync(any(Location.class))).thenReturn(cfChunk);
        // Same world
        when(Util.sameWorld(any(), any())).thenReturn(true);
        // Set up builder
        // Set the default world
        when(location.getWorld()).thenReturn(world);
        when(location.getBlock()).thenReturn(block);
        when(location.clone()).thenReturn(location);
        when(location.add(any(Vector.class))).thenReturn(location);

        // World
        when(world.getMinHeight()).thenReturn(0);
        when(world.getMaxHeight()).thenReturn(1);

        // Island
        island = new Island(location, UUID.randomUUID(), 50);

        // Plugin Island Manager
        // Default that locations are safe
        when(im.isSafeLocation(any(Location.class))).thenReturn(true);
        when(im.checkIfSafe(any(),any(),any(),any())).thenReturn(true);
        // Provide an island
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));

        // Block
        when(block.getRelative(any())).thenReturn(block);
        when(plugin.getIslands()).thenReturn(im);

        // Bukkit scheduler
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // DUT
        builder = ClosestSafeSpotTeleport.builder(plugin).entity(entity).portal()
                .location(location)
                .successRunnable(failRunnable);
        csst = builder.build();

    }

    /**
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
    }
    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#ClosestSafeSpotTeleport(world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport.Builder)}.
     */
    @Test
    public void testClosestSafeSpotTeleport() {
        assertNotNull(csst);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#checkLocation()}.
     */
    @Test
    public void testCheckLocation() {
        csst.checkLocation();
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.getScheduler();
        verify(im, times(17)).getIslandAt(location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#checkLocation()}.
     */
    @Test
    public void testCheckLocationSafeSpotImmediately() {
        // No portal
        csst = ClosestSafeSpotTeleport.builder(plugin).entity(entity).location(location).successRunnable(failRunnable).build();
        when(im.isSafeLocation(this.location)).thenReturn(true);
        csst.checkLocation();
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.getScheduler();
        verify(im, never()).getIslandAt(location);
        verify(im).isSafeLocation(location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#gatherChunks()}.
     */
    @Test
    public void testGatherChunks() {
        csst.checkLocation();
        csst.gatherChunks();
        PowerMockito.verifyStatic(Util.class, VerificationModeFactory.times(1));
        Util.getChunkAtAsync(eq(world), anyInt(), anyInt());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#getChunksToScan()}.
     */
    @Test
    public void testGetChunksToScan() {
        List<Pair<Integer, Integer>> list = csst.getChunksToScan();
        assertEquals(16, list.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#addChunk(java.util.List, world.bentobox.bentobox.util.Pair, world.bentobox.bentobox.util.Pair)}.
     */
    @Test
    public void testAddChunk() {
        Pair<Integer, Integer> chunkCoord = new Pair<>(0,0);
        Pair<Integer, Integer> chunksToScan = new Pair<>(0,0);
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        csst.addChunk(list, chunksToScan, chunkCoord);
        assertEquals(1, list.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#scanAndPopulateBlockQueue(org.bukkit.ChunkSnapshot)}.
     */
    @Test
    public void testScanAndPopulateBlockQueue() {
        csst.checkLocation();
        csst.scanAndPopulateBlockQueue(chunkSnapshot);
        assertFalse(csst.scanBlockQueue());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#finishTask()}.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testFinishTask() throws InterruptedException, ExecutionException {
        csst.checkLocation();
        csst.finishTask();
        assertFalse(builder.getResult().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#makeAndTeleport(org.bukkit.Material)}.
     */
    @Test
    public void testMakeAndTeleport() {
        csst.checkLocation();
        csst.makeAndTeleport(Material.STONE);
        verify(location, times(4)).getBlock();
        PowerMockito.verifyStatic(Util.class, VerificationModeFactory.times(1));
        Util.teleportAsync(entity, location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#scanBlockQueue()}.
     */
    @Test
    public void testScanBlockQueue() {
        csst.checkLocation();
        assertFalse(csst.scanBlockQueue());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#teleportEntity(org.bukkit.Location)}.
     */
    @Test
    public void testTeleportEntity() {
        csst.checkLocation();
        csst.teleportEntity(location);
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#asyncTeleport(org.bukkit.Location)}.
     */
    @Test
    public void testAsyncTeleport() {
        csst.checkLocation();
        csst.asyncTeleport(location);
        PowerMockito.verifyStatic(Util.class, VerificationModeFactory.times(1));
        Util.teleportAsync(entity, location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport#checkPosition(world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport.PositionData)}.
     */
    @Test
    public void testCheckPosition() {
        Vector vector = new Vector(1,2,3);
        Material block = Material.STONE;
        Material space1 = Material.AIR;
        Material space2 = Material.AIR;
        PositionData positionData = new PositionData(vector, block, space1, space2, 3);
        assertFalse(csst.checkPosition(positionData));
    }

}
