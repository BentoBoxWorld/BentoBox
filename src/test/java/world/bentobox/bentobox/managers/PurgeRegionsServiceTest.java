package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PurgeRegionsService.FilterStats;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.managers.island.IslandGrid;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;
import world.bentobox.bentobox.util.Pair;

/**
 * Direct tests for {@link PurgeRegionsService} focused on the deleted-sweep
 * path ({@link PurgeRegionsService#scanDeleted(org.bukkit.World)}) and the
 * {@code days == 0} behavior of {@link PurgeRegionsService#delete}.
 *
 * <p>These tests exercise the service directly (no command layer) so the
 * assertions stay tightly scoped to the scanning/filtering logic.
 */
class PurgeRegionsServiceTest extends CommonTestSetup {

    @Mock
    private IslandCache islandCache;
    @Mock
    private AddonsManager addonsManager;
    @Mock
    private PlayersManager pm;

    @TempDir
    Path tempDir;

    private PurgeRegionsService service;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(iwm.isNetherGenerate(world)).thenReturn(false);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isEndGenerate(world)).thenReturn(false);
        when(iwm.isEndIslands(world)).thenReturn(false);
        when(iwm.getNetherWorld(world)).thenReturn(null);
        when(iwm.getEndWorld(world)).thenReturn(null);

        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getAddonByName("Level")).thenReturn(Optional.empty());
        when(plugin.getPlayers()).thenReturn(pm);

        when(im.getIslandCache()).thenReturn(islandCache);
        when(world.getWorldFolder()).thenReturn(tempDir.toFile());

        service = new PurgeRegionsService(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * A world with no island grid returns an empty deleted-sweep result
     * rather than crashing.
     */
    @Test
    void testScanDeletedNullGrid() {
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        PurgeScanResult result = service.scanDeleted(world);
        assertTrue(result.isEmpty());
        assertEquals(0, result.days(), "days sentinel should be 0 for deleted sweep");
    }

    /**
     * A world with only non-deletable islands yields no candidate regions.
     */
    @Test
    void testScanDeletedNoDeletableIslands() {
        Island active = mock(Island.class);
        when(active.isDeletable()).thenReturn(false);

        when(islandCache.getIslands(world)).thenReturn(List.of(active));
        when(islandCache.getIslandGrid(world)).thenReturn(mock(IslandGrid.class));

        PurgeScanResult result = service.scanDeleted(world);
        assertTrue(result.isEmpty());
    }

    /**
     * A single deletable island with no neighbours produces one candidate
     * region matching its protection bounds.
     */
    @Test
    void testScanDeletedLoneDeletableIsland() {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        // Occupies r.0.0 (0..100 in X/Z)
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable));

        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new IslandData("del", 0, 0, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));

        PurgeScanResult result = service.scanDeleted(world);
        assertFalse(result.isEmpty());
        assertEquals(1, result.deletableRegions().size());
        assertEquals(0, result.days());
    }

    /**
     * An island straddling two regions (X = 500..700 crosses the r.0/r.1
     * boundary at X=512) produces two candidate region entries.
     */
    @Test
    void testScanDeletedIslandStraddlesRegionBoundary() {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMinProtectedX()).thenReturn(500);
        when(deletable.getMaxProtectedX()).thenReturn(700);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new IslandData("del", 500, 0, 200)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));

        PurgeScanResult result = service.scanDeleted(world);
        assertEquals(2, result.deletableRegions().size(),
                "Island straddling r.0.0 and r.1.0 should produce two candidate regions");
    }

    /**
     * Strict filter: a region containing one deletable and one non-deletable
     * island must be dropped.
     */
    @Test
    void testScanDeletedStrictFilterDropsMixedRegion() {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        Island active = mock(Island.class);
        when(active.getUniqueId()).thenReturn("act");
        when(active.isDeletable()).thenReturn(false);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable, active));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new IslandData("del", 0, 0, 100),
                        new IslandData("act", 200, 200, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));
        when(im.getIslandById("act")).thenReturn(Optional.of(active));

        PurgeScanResult result = service.scanDeleted(world);
        assertTrue(result.isEmpty(), "Mixed region must be blocked by strict filter");
    }

    /**
     * Missing island rows in the grid must not block the region — they're
     * already gone and count as "no blocker".
     */
    @Test
    void testScanDeletedMissingIslandRowDoesNotBlock() {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new IslandData("del", 0, 0, 100),
                        new IslandData("ghost", 300, 300, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));
        when(im.getIslandById("ghost")).thenReturn(Optional.empty());

        PurgeScanResult result = service.scanDeleted(world);
        assertFalse(result.isEmpty(), "Ghost island (no DB row) must not block the reap");
        assertEquals(1, result.deletableRegions().size());
    }

    /**
     * {@code delete} with a {@code days == 0} scan must bypass the freshness
     * recheck — a region file touched seconds ago must still be reaped.
     * This is the core of the deleted-sweep semantics.
     */
    @Test
    void testDeleteWithZeroDaysBypassesFreshnessCheck() throws IOException {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMemberSet()).thenReturn(ImmutableSet.<UUID>of());
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new IslandData("del", 0, 0, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));
        when(im.deleteIslandId("del")).thenReturn(true);
        // deletePlayerFromWorldFolder iterates members (empty set here) so
        // getIslands(World, UUID) is never reached — no stub needed.

        // Create a fresh .mca file — timestamp is "now". The age sweep would
        // skip this file; the deleted sweep must reap it anyway.
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Path regionFile = regionDir.resolve("r.0.0.mca");
        byte[] data = new byte[8192];
        int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < 1024; i++) {
            int offset = 4096 + i * 4;
            data[offset]     = (byte) (nowSeconds >> 24);
            data[offset + 1] = (byte) (nowSeconds >> 16);
            data[offset + 2] = (byte) (nowSeconds >> 8);
            data[offset + 3] = (byte)  nowSeconds;
        }
        Files.write(regionFile, data);

        PurgeScanResult scan = service.scanDeleted(world);
        assertFalse(scan.isEmpty());

        boolean ok = service.delete(scan);
        assertTrue(ok, "delete() should return true for a fresh-timestamp region under the deleted sweep");
        assertFalse(regionFile.toFile().exists(),
                "Fresh region file must be reaped when days == 0");
    }

    /**
     * {@code evictChunks} must walk the full 32x32 chunk square inside each
     * target region and call {@code unloadChunk(cx, cz, false)} on every chunk
     * that is currently loaded. This is the fix for the bug where reaped
     * region files were re-flushed by Paper's autosave because the in-memory
     * chunks survived the disk delete.
     */
    @Test
    void testEvictChunksUnloadsLoadedChunksInTargetRegion() {
        // Build a scan result with a single target region at r.0.0 — chunks
        // (0,0) .. (31,31). Mark only (5,7) and (10,12) as currently loaded
        // so we can prove the call is gated on isChunkLoaded.
        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("del"));
        PurgeScanResult scan = new PurgeScanResult(world, 0, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        when(world.isChunkLoaded(5, 7)).thenReturn(true);
        when(world.isChunkLoaded(10, 12)).thenReturn(true);
        when(world.unloadChunk(anyInt(), anyInt(), eq(false))).thenReturn(true);

        service.evictChunks(scan);

        // Sweep covers all 32*32 = 1024 chunk coordinates exactly once.
        verify(world, times(1024)).isChunkLoaded(anyInt(), anyInt());
        // Only the two loaded chunks were unloaded.
        verify(world).unloadChunk(5, 7, false);
        verify(world).unloadChunk(10, 12, false);
        verify(world, times(2)).unloadChunk(anyInt(), anyInt(), eq(false));
    }

    /**
     * Region coordinates must translate to chunk coordinates via {@code << 5}
     * (each region holds 32×32 chunks). r.1.-1 → chunks (32..63, -32..-1).
     */
    @Test
    void testEvictChunksUsesCorrectChunkCoordsForNonZeroRegion() {
        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(1, -1), Set.of("del"));
        PurgeScanResult scan = new PurgeScanResult(world, 0, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        // The bottom-left corner of r.1.-1 is (32, -32); the top-right is (63, -1).
        when(world.isChunkLoaded(32, -32)).thenReturn(true);
        when(world.isChunkLoaded(63, -1)).thenReturn(true);
        when(world.unloadChunk(anyInt(), anyInt(), eq(false))).thenReturn(true);

        service.evictChunks(scan);

        verify(world).unloadChunk(32, -32, false);
        verify(world).unloadChunk(63, -1, false);
        // Coordinates outside the region (e.g. (0,0)) must never be checked.
        verify(world, never()).isChunkLoaded(0, 0);
        verify(world, never()).isChunkLoaded(31, -1);
    }

    /**
     * When an island's protection box extends beyond the reaped region(s)
     * and one of its non-reaped regions still has an {@code r.X.Z.mca} file
     * on disk, {@code delete} must <em>not</em> remove the island DB row —
     * residual blocks exist with no other cleanup path. The next purge cycle
     * will retry.
     */
    @Test
    void testDeleteDefersDBRowWhenResidualRegionExists() throws IOException {
        // Age sweep (days=30): island spans X=0..1000 (crosses r.0 and r.1)
        // but only r.0.0 is in the scan — the strict filter blocked r.1.0
        // because of an active neighbour. r.1.0.mca stays on disk.
        Island spans = mock(Island.class);
        when(spans.getUniqueId()).thenReturn("spans");
        when(spans.isDeletable()).thenReturn(true);
        when(spans.getMinProtectedX()).thenReturn(0);
        when(spans.getMaxProtectedX()).thenReturn(1000);
        when(spans.getMinProtectedZ()).thenReturn(0);
        when(spans.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("spans")).thenReturn(Optional.of(spans));

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Path reaped = regionDir.resolve("r.0.0.mca");
        Path residual = regionDir.resolve("r.1.0.mca");
        Files.write(reaped, new byte[0]);
        Files.write(residual, new byte[0]);

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("spans"));
        PurgeScanResult scan = new PurgeScanResult(world, 30, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        boolean ok = service.delete(scan);
        assertTrue(ok);
        assertFalse(reaped.toFile().exists());
        assertTrue(residual.toFile().exists());
        // Age sweep: DB row must NOT be removed while residual region exists.
        verify(im, never()).deleteIslandId(anyString());
        verify(islandCache, never()).deleteIslandFromCache(anyString());
    }

    /**
     * Age sweep (days > 0): when every region the island's bounds touch is
     * absent from disk after the reap, the DB row must be removed immediately.
     */
    @Test
    void testDeleteRemovesDBRowWhenAllRegionsGone() throws IOException {
        Island tiny = mock(Island.class);
        when(tiny.getUniqueId()).thenReturn("tiny");
        when(tiny.isDeletable()).thenReturn(true);
        when(tiny.getMemberSet()).thenReturn(ImmutableSet.<UUID>of());
        // Fits entirely in r.0.0
        when(tiny.getMinProtectedX()).thenReturn(0);
        when(tiny.getMaxProtectedX()).thenReturn(100);
        when(tiny.getMinProtectedZ()).thenReturn(0);
        when(tiny.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("tiny")).thenReturn(Optional.of(tiny));
        when(im.deleteIslandId("tiny")).thenReturn(true);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.write(regionDir.resolve("r.0.0.mca"), new byte[0]);

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("tiny"));
        PurgeScanResult scan = new PurgeScanResult(world, 30, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        boolean ok = service.delete(scan);
        assertTrue(ok);
        verify(im, times(1)).deleteIslandId("tiny");
        verify(islandCache, times(1)).deleteIslandFromCache("tiny");
    }

    /**
     * Age sweep: a mixed batch where one island is fully reaped and another
     * has a residual region — only the fully-reaped island's DB row is removed.
     */
    @Test
    void testDeleteDefersOnlySomeIslandsInMixedBatch() throws IOException {
        Island tiny = mock(Island.class);
        when(tiny.getUniqueId()).thenReturn("tiny");
        when(tiny.isDeletable()).thenReturn(true);
        when(tiny.getMemberSet()).thenReturn(ImmutableSet.<UUID>of());
        when(tiny.getMinProtectedX()).thenReturn(0);
        when(tiny.getMaxProtectedX()).thenReturn(100);
        when(tiny.getMinProtectedZ()).thenReturn(0);
        when(tiny.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("tiny")).thenReturn(Optional.of(tiny));
        when(im.deleteIslandId("tiny")).thenReturn(true);

        Island spans = mock(Island.class);
        when(spans.getUniqueId()).thenReturn("spans");
        when(spans.isDeletable()).thenReturn(true);
        when(spans.getMinProtectedX()).thenReturn(0);
        when(spans.getMaxProtectedX()).thenReturn(1000);
        when(spans.getMinProtectedZ()).thenReturn(0);
        when(spans.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("spans")).thenReturn(Optional.of(spans));

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.write(regionDir.resolve("r.0.0.mca"), new byte[0]);
        Files.write(regionDir.resolve("r.1.0.mca"), new byte[0]); // residual for "spans"

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("tiny", "spans"));
        PurgeScanResult scan = new PurgeScanResult(world, 30, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        boolean ok = service.delete(scan);
        assertTrue(ok);
        verify(im, times(1)).deleteIslandId("tiny");
        verify(im, never()).deleteIslandId("spans");
        verify(islandCache, times(1)).deleteIslandFromCache("tiny");
        verify(islandCache, never()).deleteIslandFromCache("spans");
    }

    /**
     * An empty scan must short-circuit — no chunk-loaded probes at all.
     */
    @Test
    void testEvictChunksEmptyScanIsNoop() {
        PurgeScanResult scan = new PurgeScanResult(world, 0, new HashMap<>(), false, false,
                new FilterStats(0, 0, 0, 0));

        service.evictChunks(scan);

        verify(world, never()).isChunkLoaded(anyInt(), anyInt());
        verify(world, never()).unloadChunk(anyInt(), anyInt(), eq(false));
    }

    // ------------------------------------------------------------------
    // Deferred deletion (deleted sweep, days == 0)
    // ------------------------------------------------------------------

    /**
     * Deleted sweep (days=0): DB row removal must be deferred to shutdown,
     * not executed immediately. The island ID goes into pendingDeletions.
     */
    @Test
    void testDeletedSweepDefersDBDeletionToShutdown() throws IOException {
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del1");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("del1")).thenReturn(Optional.of(deletable));

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.write(regionDir.resolve("r.0.0.mca"), new byte[0]);

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("del1"));
        PurgeScanResult scan = new PurgeScanResult(world, 0, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        boolean ok = service.delete(scan);
        assertTrue(ok);
        // DB row must NOT be removed immediately — deferred to shutdown.
        verify(im, never()).deleteIslandId(anyString());
        verify(islandCache, never()).deleteIslandFromCache(anyString());
        // Island ID must be in pending set.
        assertTrue(service.getPendingDeletions().contains("del1"));
    }

    /**
     * {@link PurgeRegionsService#flushPendingDeletions()} must process all
     * deferred island IDs and clear the pending set.
     */
    @Test
    void testFlushPendingDeletionsRemovesIslands() throws IOException {
        Island del1 = mock(Island.class);
        when(del1.getUniqueId()).thenReturn("del1");
        when(del1.isDeletable()).thenReturn(true);
        when(del1.getMinProtectedX()).thenReturn(0);
        when(del1.getMaxProtectedX()).thenReturn(100);
        when(del1.getMinProtectedZ()).thenReturn(0);
        when(del1.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("del1")).thenReturn(Optional.of(del1));
        when(im.deleteIslandId("del1")).thenReturn(true);

        Island del2 = mock(Island.class);
        when(del2.getUniqueId()).thenReturn("del2");
        when(del2.isDeletable()).thenReturn(true);
        when(del2.getMinProtectedX()).thenReturn(512);
        when(del2.getMaxProtectedX()).thenReturn(612);
        when(del2.getMinProtectedZ()).thenReturn(0);
        when(del2.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("del2")).thenReturn(Optional.of(del2));
        when(im.deleteIslandId("del2")).thenReturn(true);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.write(regionDir.resolve("r.0.0.mca"), new byte[0]);
        Files.write(regionDir.resolve("r.1.0.mca"), new byte[0]);

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("del1"));
        regions.put(new Pair<>(1, 0), Set.of("del2"));
        PurgeScanResult scan = new PurgeScanResult(world, 0, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        service.delete(scan);
        // Both deferred — not yet deleted.
        verify(im, never()).deleteIslandId(anyString());
        assertEquals(2, service.getPendingDeletions().size());

        // Now flush (simulates plugin shutdown).
        service.flushPendingDeletions();
        verify(im, times(1)).deleteIslandId("del1");
        verify(im, times(1)).deleteIslandId("del2");
        verify(islandCache, times(1)).deleteIslandFromCache("del1");
        verify(islandCache, times(1)).deleteIslandFromCache("del2");
        assertTrue(service.getPendingDeletions().isEmpty());
    }

    /**
     * Age sweep (days > 0) must still delete DB rows immediately when all
     * regions are gone from disk — no deferral to shutdown.
     */
    @Test
    void testAgeSweepStillDeletesImmediately() throws IOException {
        Island tiny = mock(Island.class);
        when(tiny.getUniqueId()).thenReturn("tiny");
        when(tiny.isDeletable()).thenReturn(true);
        when(tiny.getMemberSet()).thenReturn(ImmutableSet.<UUID>of());
        when(tiny.getMinProtectedX()).thenReturn(0);
        when(tiny.getMaxProtectedX()).thenReturn(100);
        when(tiny.getMinProtectedZ()).thenReturn(0);
        when(tiny.getMaxProtectedZ()).thenReturn(100);
        when(im.getIslandById("tiny")).thenReturn(Optional.of(tiny));
        when(im.deleteIslandId("tiny")).thenReturn(true);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.write(regionDir.resolve("r.0.0.mca"), new byte[0]);

        Map<Pair<Integer, Integer>, Set<String>> regions = new HashMap<>();
        regions.put(new Pair<>(0, 0), Set.of("tiny"));
        PurgeScanResult scan = new PurgeScanResult(world, 30, regions, false, false,
                new FilterStats(0, 0, 0, 0));

        service.delete(scan);
        // Age sweep: immediate deletion, not deferred.
        verify(im, times(1)).deleteIslandId("tiny");
        assertTrue(service.getPendingDeletions().isEmpty());
    }
}
