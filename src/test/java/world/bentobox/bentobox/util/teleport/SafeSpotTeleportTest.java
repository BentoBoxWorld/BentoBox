package world.bentobox.bentobox.util.teleport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
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
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Test class for safe teleporting
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class, Bukkit.class})
public class SafeSpotTeleportTest {

    // Class under test
    private SafeSpotTeleport sst;

    @Mock
    private SafeSpotTeleport.Builder builder;
    @Mock
    private BentoBox plugin;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private Entity entity;

    private boolean portal;

    private int num;

    private String name;
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
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Setup instance
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // IWM
        when(iwm.getIslandProtectionRange(any())).thenReturn(100);
        when(iwm.getIslandDistance(any())).thenReturn(400);
        when(plugin.getIWM()).thenReturn(iwm);

        // Mock static Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getChunkAtAsync(any(Location.class))).thenReturn(cfChunk);
        // Same world
        when(Util.sameWorld(any(), any())).thenReturn(true);
        // Set up a mock builder
        when(builder.getPlugin()).thenReturn(plugin);
        when(builder.getEntity()).thenReturn(entity);
        when(builder.getLocation()).thenReturn(location);
        when(builder.isPortal()).thenReturn(portal);
        when(builder.getHomeNumber()).thenReturn(num);
        when(builder.getHomeName()).thenReturn(name);
        when(builder.getRunnable()).thenReturn(runnable);
        when(builder.getFailRunnable()).thenReturn(failRunnable);
        when(builder.getResult()).thenReturn(result);
        // Set the default world
        when(location.getWorld()).thenReturn(world);

        // Island
        island = new Island(location, UUID.randomUUID(), 50);

        // Plugin Island Manager
        // Default that locations are safe
        when(im.isSafeLocation(any(Location.class))).thenReturn(true);
        // Provide an island
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));


        when(plugin.getIslands()).thenReturn(im);

        // Bukkit scheduler
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#SafeSpotTeleport(world.bentobox.bentobox.util.teleport.SafeSpotTeleport.Builder)}.
     */
    @Test(expected = NullPointerException.class)
    public void testSafeSpotTeleportNullWorld() {
        when(location.getWorld()).thenReturn(null);
        sst = new SafeSpotTeleport(builder);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#SafeSpotTeleport(world.bentobox.bentobox.util.teleport.SafeSpotTeleport.Builder)}.
     */
    @Test
    public void testSafeSpotTeleport() {
        sst = new SafeSpotTeleport(builder);
        verify(cfChunk).thenRun(any(Runnable.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#tryToGo(java.lang.String)}.
     */
    @Test
    public void testTryToGoSafeNotPortal() {
        portal = false;
        testSafeSpotTeleport();
        sst.tryToGo("failure message");
        PowerMockito.verifyStatic(Util.class);
        // Verify that the teleport is done immediately
        Util.teleportAsync(entity, location);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#tryToGo(java.lang.String)}.
     */
    @Test
    public void testTryToGoUnsafe() {
        when(im.isSafeLocation(any(Location.class))).thenReturn(false);
        // Set up fields
        testSafeSpotTeleport();
        sst.tryToGo("failure message");
        verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(1L));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#gatherChunks(java.lang.String)}.
     */
    @Test
    public void testGatherChunks() {
        // Setup fields
        testTryToGoUnsafe();
        // run test
        assertTrue(sst.gatherChunks("failure message"));
        PowerMockito.verifyStatic(Util.class);
        Util.getChunkAtAsync(eq(world), anyInt(), anyInt());
        // run test again - should be blocked because of atomic boolean
        assertFalse(sst.gatherChunks("failure message"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#tidyUp(org.bukkit.entity.Entity, java.lang.String)}.
     */
    @Test
    public void testTidyUpNoPlayerFailRunnable() {
        when(im.isSafeLocation(any(Location.class))).thenReturn(false);
        sst = new SafeSpotTeleport(builder);
        sst.tryToGo("failure message");
        sst.tidyUp(entity, "failure note");
        verify(task).cancel();
        verify(scheduler).runTask(plugin, failRunnable);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#tidyUp(org.bukkit.entity.Entity, java.lang.String)}.
     */
    @Test
    public void testTidyUpPlayer() {
        when(im.isSafeLocation(any(Location.class))).thenReturn(false);
        sst = new SafeSpotTeleport(builder);
        sst.tryToGo("failure message");
        sst.tidyUp(entity, "failure note");
        verify(task).cancel();
        verify(scheduler).runTask(plugin, failRunnable);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#makeAndTeleport(org.bukkit.Material)}.
     */
    @Test
    public void testMakeAndTeleport() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#getChunksToScan()}.
     */
    @Test
    public void testGetChunksToScan() {
        testSafeSpotTeleport();
        List<Pair<Integer, Integer>> pairs = sst.getChunksToScan();
        assertEquals(62, pairs.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#scanChunk(org.bukkit.ChunkSnapshot)}.
     */
    @Test
    public void testScanChunk() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#teleportEntity(org.bukkit.Location)}.
     */
    @Test
    public void testTeleportEntity() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#checkBlock(org.bukkit.ChunkSnapshot, int, int, int)}.
     */
    @Test
    public void testCheckBlock() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#safe(org.bukkit.ChunkSnapshot, int, int, int, org.bukkit.World)}.
     */
    @Test
    public void testSafe() {
        //fail("Not yet implemented"); // TODO
    }

}
