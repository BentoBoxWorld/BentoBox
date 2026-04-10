package world.bentobox.bentobox.util.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link SafeSpotTeleport}.
 */
class SafeSpotTeleportTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Location setup
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(64);
        when(location.getBlockZ()).thenReturn(0);
        when(location.clone()).thenReturn(location);

        // World setup
        when(world.getMaxHeight()).thenReturn(320);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Island setup
        when(island.getProtectionRange()).thenReturn(50);
        when(island.getProtectionCenter()).thenReturn(location);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(island.inIslandSpace(any(Pair.class))).thenReturn(true);

        // Safe location check
        when(im.isSafeLocation(any(Location.class))).thenReturn(false);
        when(im.checkIfSafe(any(World.class), any(Material.class), any(Material.class), any(Material.class))).thenReturn(false);

        // Scheduler - return a mock task for the timer
        BukkitTask mockTask = mock(BukkitTask.class);
        when(sch.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(mockTask);

        // Util.getChunkAtAsync - complete immediately
        mockedUtil.when(() -> Util.getChunkAtAsync(any(Location.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Settings - getSafeSpotSearchVerticalRange defaults to 400 in Settings
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---- Builder validation ----

    @Test
    void testBuilderBuildNullEntity() {
        SafeSpotTeleport result = new SafeSpotTeleport.Builder(plugin)
                .location(location)
                .build();
        assertNull(result);
        verify(plugin).logError("Attempt to safe teleport a null entity!");
    }

    @Test
    void testBuilderBuildNullLocation() {
        SafeSpotTeleport result = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .build();
        assertNull(result);
        verify(plugin).logError("Attempt to safe teleport to a null location!");
    }

    @Test
    void testBuilderBuildNullWorld() {
        Location noWorld = mock(Location.class);
        when(noWorld.getWorld()).thenReturn(null);
        SafeSpotTeleport result = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(noWorld)
                .build();
        assertNull(result);
        verify(plugin).logError("Attempt to safe teleport to a null world!");
    }

    @Test
    void testBuilderBuildValid() {
        SafeSpotTeleport result = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();
        assertNotNull(result);
    }

    @Test
    void testBuilderBuildFuture() {
        CompletableFuture<Boolean> future = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .buildFuture();
        assertNotNull(future);
    }

    @Test
    void testBuilderBuildFutureNullEntity() {
        CompletableFuture<Boolean> future = new SafeSpotTeleport.Builder(plugin)
                .location(location)
                .buildFuture();
        // Should complete with null since entity is null
        assertTrue(future.isDone());
        assertNull(future.join());
    }

    // ---- Builder setters / getters ----

    @Test
    void testBuilderPortal() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertFalse(builder.isPortal());
        builder.portal();
        assertTrue(builder.isPortal());
    }

    @Test
    void testBuilderHomeName() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertEquals("", builder.getHomeName());
        builder.homeName("myHome");
        assertEquals("myHome", builder.getHomeName());
    }

    @Test
    void testBuilderCancelIfFail() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertFalse(builder.isCancelIfFail());
        builder.cancelIfFail(true);
        assertTrue(builder.isCancelIfFail());
    }

    @Test
    void testBuilderFailureMessage() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertEquals("", builder.getFailureMessage());
        builder.failureMessage("custom.message");
        assertEquals("custom.message", builder.getFailureMessage());
    }

    @Test
    void testBuilderThenRun() {
        Runnable r = mock(Runnable.class);
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertNull(builder.getRunnable());
        builder.thenRun(r);
        assertEquals(r, builder.getRunnable());
    }

    @Test
    void testBuilderIfFail() {
        Runnable r = mock(Runnable.class);
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        assertNull(builder.getFailRunnable());
        builder.ifFail(r);
        assertEquals(r, builder.getFailRunnable());
    }

    @Test
    void testBuilderIsland() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin);
        builder.island(island);
        assertEquals(location, builder.getLocation());
    }

    @Test
    void testBuilderDefaultFailureMessageForPlayer() {
        // When entity is a Player and failureMessage is empty, build() should set a default
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location);
        builder.build();
        assertEquals("general.errors.no-safe-location-found", builder.getFailureMessage());
    }

    @Test
    void testBuilderCustomFailureMessageForPlayer() {
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .failureMessage("custom.fail");
        builder.build();
        assertEquals("custom.fail", builder.getFailureMessage());
    }

    @Test
    void testBuilderNonPlayerEntityNoDefaultMessage() {
        Entity entity = mock(Entity.class);
        SafeSpotTeleport.Builder builder = new SafeSpotTeleport.Builder(plugin)
                .entity(entity)
                .location(location);
        builder.build();
        // Non-player entities should keep empty failure message
        assertEquals("", builder.getFailureMessage());
    }

    // ---- tryToGo - safe location ----

    @Test
    void testTryToGoSafeLocation() {
        // If location is already safe, should teleport immediately
        when(im.isSafeLocation(location)).thenReturn(true);
        mockedUtil.when(() -> Util.teleportAsync(any(Entity.class), any(Location.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();
        assertNotNull(sst);
    }

    @Test
    void testTryToGoSafeLocationPortalMode() {
        // In portal mode, safe location becomes bestSpot, then starts scanning
        when(im.isSafeLocation(location)).thenReturn(true);

        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .portal()
                .build();
        assertNotNull(sst);
    }

    // ---- getChunksToScan ----

    @Test
    void testGetChunksToScan() {
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();

        List<Pair<Integer, Integer>> chunks = sst.getChunksToScan();
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
    }

    // ---- checkBlock / scanChunk ----

    @Test
    void testCheckBlockUnsafe() {
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();

        ChunkSnapshot chunk = mock(ChunkSnapshot.class);
        when(chunk.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.STONE);
        // checkIfSafe returns false by default

        assertFalse(sst.checkBlock(chunk, 0, 64, 0));
    }

    @Test
    void testCheckBlockPortalFound() {
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .portal()
                .build();

        ChunkSnapshot chunk = mock(ChunkSnapshot.class);
        when(chunk.getBlockType(0, 64, 0)).thenReturn(Material.STONE);
        when(chunk.getBlockType(0, 65, 0)).thenReturn(Material.NETHER_PORTAL);
        when(chunk.getBlockType(0, 66, 0)).thenReturn(Material.NETHER_PORTAL);

        // Not safe, but portal detection should toggle portal mode off
        assertFalse(sst.checkBlock(chunk, 0, 64, 0));
    }

    @Test
    void testScanChunkNoSafeSpot() {
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();

        ChunkSnapshot chunk = mock(ChunkSnapshot.class);
        when(chunk.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.LAVA);
        when(chunk.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(64);

        assertFalse(sst.scanChunk(chunk));
    }

    @Test
    void testScanChunkSafeSpotFound() {
        // Mock teleportAsync for the teleport that happens when a safe spot is found
        mockedUtil.when(() -> Util.teleportAsync(any(Entity.class), any(Location.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        BukkitTask mockTask = mock(BukkitTask.class);
        when(sch.runTask(any(), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mockTask;
        });

        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .build();

        ChunkSnapshot chunk = mock(ChunkSnapshot.class);
        when(chunk.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        when(chunk.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(64);
        // Make checkIfSafe return true for the first block
        when(im.checkIfSafe(any(World.class), any(Material.class), any(Material.class), any(Material.class))).thenReturn(true);

        assertTrue(sst.scanChunk(chunk));
    }

    @Test
    void testScanChunkPortalModeStashesBestSpot() {
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(mockPlayer)
                .location(location)
                .portal()
                .build();

        ChunkSnapshot chunk = mock(ChunkSnapshot.class);
        when(chunk.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        when(chunk.getHighestBlockYAt(anyInt(), anyInt())).thenReturn(64);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(im.checkIfSafe(any(World.class), any(Material.class), any(Material.class), any(Material.class))).thenReturn(true);

        // In portal mode, safe() returns false (stashes bestSpot instead of teleporting)
        assertFalse(sst.scanChunk(chunk));
    }

    // ---- Builder with non-player entity ----

    @Test
    void testBuildWithNonPlayerEntity() {
        Entity entity = mock(Entity.class);
        SafeSpotTeleport sst = new SafeSpotTeleport.Builder(plugin)
                .entity(entity)
                .location(location)
                .build();
        assertNotNull(sst);
    }
}
