package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        assertEquals(1, result.deleteableRegions().size());
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
        assertEquals(2, result.deleteableRegions().size(),
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
        assertEquals(1, result.deleteableRegions().size());
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
}
