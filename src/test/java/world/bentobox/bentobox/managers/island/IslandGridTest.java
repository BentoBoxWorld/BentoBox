package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;

/**
 * Grid test
 */
public class IslandGridTest extends CommonTestSetup {

    private IslandGrid ig;
    @Mock
    private IslandCache im;
    @Mock
    private Island island2;
    @Mock
    private Island overlappingIsland;
    @Mock
    private Island original;
    @Mock
    private Island updated;
    @Mock
    private Island a;
    @Mock
    private Island b;
    @Mock
    private Island big;
    @Mock
    private Island small;
    @Mock
    private Island zIsland;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Islands
        when(island.getMinX()).thenReturn(356);
        when(island.getMinZ()).thenReturn(5678);
        when(island.getRange()).thenReturn(64);
        when(island.getUniqueId()).thenReturn("island");
        when(overlappingIsland.getMinX()).thenReturn(360);
        when(overlappingIsland.getMinZ()).thenReturn(5678);
        when(overlappingIsland.getRange()).thenReturn(64);
        when(overlappingIsland.getUniqueId()).thenReturn("overlappingIsland");
        when(island2.getMinX()).thenReturn(-32);
        when(island2.getMinZ()).thenReturn(-32);
        when(island2.getRange()).thenReturn(64);
        when(island2.getUniqueId()).thenReturn("island2");
        when(im.getIslandById("island")).thenReturn(island);
        when(im.getIslandById("island2")).thenReturn(island2);
        when(im.getIslandById("overlappingIsland")).thenReturn(overlappingIsland);
        ig = new IslandGrid(im);
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
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#addToGrid(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    void testAddToGrid() {
       assertTrue(ig.addToGrid(island));
       assertFalse(ig.addToGrid(overlappingIsland));
       assertTrue(ig.addToGrid(island2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#removeFromGrid(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    void testRemoveFromGrid() {
        assertTrue(ig.addToGrid(island));
       assertTrue(ig.removeFromGrid(island));
       assertFalse(ig.removeFromGrid(island2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getIslandAt(int, int)}.
     */
    @Test
    void testGetIslandAt() {
        assertNull(ig.getIslandAt(0, 0));
        assertTrue(ig.addToGrid(island));
        assertTrue(ig.addToGrid(island2));
        assertEquals(island, ig.getIslandAt(360, 5700));
        assertEquals(island2, ig.getIslandAt(0, 0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#isIslandAt(int, int)}.
     */
    @Test
    void testIsIslandAt() {
        assertFalse(ig.isIslandAt(0, 0));
        assertTrue(ig.addToGrid(island2));
        assertTrue(ig.isIslandAt(0, 0));
        assertTrue(ig.addToGrid(island));
        assertTrue(ig.isIslandAt(360, 5700));
        assertFalse(ig.isIslandAt(-1000, 1000));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getIslandStringAt(int, int)}.
     */
    @Test
    void testGetIslandStringAt() {
       assertNull(ig.getIslandStringAt(0, 0));
       assertTrue(ig.addToGrid(island2));
       assertEquals("island2", ig.getIslandStringAt(0, 0));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getSize()}.
     */
    @Test
    void testGetSize() {
        assertEquals(0, ig.getSize());
        assertTrue(ig.addToGrid(island2));
        assertEquals(1, ig.getSize());
        assertTrue(ig.addToGrid(island));
        assertEquals(2, ig.getSize());
        ig.removeFromGrid(island);
        assertEquals(1, ig.getSize());
         ig.removeFromGrid(island2);
         assertEquals(0, ig.getSize());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getAllIslands()}.
     */
    @Test
    void testGetAllIslands() {
        assertNotNull(ig.getAllIslands());
        assertTrue(ig.getAllIslands().isEmpty());
        ig.addToGrid(island);
        assertEquals(1, ig.getAllIslands().size());
    }

    @Test
    void testUpdateIslandCoordinatesKeepsSingleEntry() {
        // original at (100,100) range 20
        when(original.getMinX()).thenReturn(100);
        when(original.getMinZ()).thenReturn(100);
        when(original.getRange()).thenReturn(20);
        when(original.getUniqueId()).thenReturn("orig");

        // updated has same id but moved to (300,300) range 20
        when(updated.getMinX()).thenReturn(300);
        when(updated.getMinZ()).thenReturn(300);
        when(updated.getRange()).thenReturn(20);
        when(updated.getUniqueId()).thenReturn("orig");

        when(im.getIslandById("orig")).thenReturn(original);

        // add original
        assertTrue(ig.addToGrid(original));
        assertEquals(1, ig.getSize());

        // add updated (same id) -> should update, keep size 1
        assertTrue(ig.addToGrid(updated));
        assertEquals(1, ig.getSize());

        // original location should no longer contain the island
        assertNull(ig.getIslandStringAt(110, 110));

        // new location should contain the island
        assertEquals("orig", ig.getIslandStringAt(310, 310));
    }

    @Test
    void testAdjacentIslandsAllowedWhenEdgesTouch() {
        // island a covers x:[0,20) z:[0,20)
        when(a.getMinX()).thenReturn(0);
        when(a.getMinZ()).thenReturn(0);
        when(a.getRange()).thenReturn(10);
        when(a.getUniqueId()).thenReturn("a");

        // island b starts exactly at x=20 (touching edge), z same
        when(b.getMinX()).thenReturn(20);
        when(b.getMinZ()).thenReturn(0);
        when(b.getRange()).thenReturn(10);
        when(b.getUniqueId()).thenReturn("b");

        when(im.getIslandById("a")).thenReturn(a);
        when(im.getIslandById("b")).thenReturn(b);

        assertTrue(ig.addToGrid(a));
        // touching edge should be allowed
        assertTrue(ig.addToGrid(b));

        // verify both retrievable at representative coords
        assertEquals("a", ig.getIslandStringAt(10, 10));
        assertEquals("b", ig.getIslandStringAt(21, 10));
    }

    @Test
    void testLargeExistingIslandShouldBlockSmallIslandEvenIfMinXOutsideSubMapWindow() {
        // big island minX = 0, range = 1000
        when(big.getMinX()).thenReturn(0);
        when(big.getMinZ()).thenReturn(0);
        when(big.getRange()).thenReturn(1000);
        when(big.getUniqueId()).thenReturn("big");

        // small island minX = 1500, range = 10 -> would overlap big
        when(small.getMinX()).thenReturn(1500);
        when(small.getMinZ()).thenReturn(10);
        when(small.getRange()).thenReturn(10);
        when(small.getUniqueId()).thenReturn("small");

        when(im.getIslandById("big")).thenReturn(big);
        when(im.getIslandById("small")).thenReturn(small);

        assertTrue(ig.addToGrid(big));

        // Expected: adding small should be rejected because it lies inside big
        assertFalse(ig.addToGrid(small), "Small island overlaps big island; should have been rejected");
    }

    @Test
    void testGetIslandStringAtWhenXEntryExistsButNoZEntryApplies() {
        // island exists at minX=100 minZ=100 range=10 (covers z [100,120))
        when(zIsland.getMinX()).thenReturn(100);
        when(zIsland.getMinZ()).thenReturn(100);
        when(zIsland.getRange()).thenReturn(10);
        when(zIsland.getUniqueId()).thenReturn("z");

        when(im.getIslandById("z")).thenReturn(zIsland);

        assertTrue(ig.addToGrid(zIsland));

        // Query an x within island x-range but z is below any minZ -> should return null
        assertNull(ig.getIslandStringAt(110, 50));
    }

    // ---- New tests for spatial hash correctness ----

    /**
     * Tests that a large island at a low X is found even when a smaller island exists
     * at a closer X. This was a correctness bug in the old floorEntry-based lookup.
     */
    @Test
    void testLookupCorrectnessWithArbitraryPositions() {
        // Large island at x=0, covers x:[0, 400)
        Island largeIsland = createMockIsland("large", 0, 0, 200);
        when(im.getIslandById("large")).thenReturn(largeIsland);

        // Small island at x=500, different Z to avoid overlap, covers x:[500, 520)
        Island smallIsland = createMockIsland("small-far", 500, 500, 10);
        when(im.getIslandById("small-far")).thenReturn(smallIsland);

        assertTrue(ig.addToGrid(largeIsland));
        assertTrue(ig.addToGrid(smallIsland));

        // Query x=350 — inside large island, past small island's minX position
        // The old floorEntry approach would have checked smallIsland's X bucket and missed largeIsland
        assertEquals("large", ig.getIslandStringAt(350, 100));
        assertEquals("small-far", ig.getIslandStringAt(510, 510));
    }

    /**
     * Tests bulk load performance — 10,000 islands should complete quickly.
     */
    @Test
    void testBulkLoadPerformance() {
        int count = 10_000;
        int spacing = 200; // each island: range=50, diameter=100, spacing=200 (no overlap)
        for (int i = 0; i < count; i++) {
            int x = (i % 100) * spacing;
            int z = (i / 100) * spacing;
            String id = "island-" + i;
            Island mockIsland = createMockIsland(id, x, z, 50);
            ig.addToGrid(mockIsland);
        }
        assertEquals(count, ig.getSize());

        // Verify a few lookups work
        assertEquals("island-0", ig.getIslandStringAt(25, 25));
        assertEquals("island-99", ig.getIslandStringAt(99 * spacing + 25, 25));
    }

    /**
     * Tests islands in all four quadrants (negative and positive coordinates).
     */
    @Test
    void testNegativeCoordinates() {
        Island ne = createMockIsland("ne", 100, 100, 50);
        Island nw = createMockIsland("nw", -200, 100, 50);
        Island se = createMockIsland("se", 100, -200, 50);
        Island sw = createMockIsland("sw", -200, -200, 50);

        when(im.getIslandById("ne")).thenReturn(ne);
        when(im.getIslandById("nw")).thenReturn(nw);
        when(im.getIslandById("se")).thenReturn(se);
        when(im.getIslandById("sw")).thenReturn(sw);

        assertTrue(ig.addToGrid(ne));
        assertTrue(ig.addToGrid(nw));
        assertTrue(ig.addToGrid(se));
        assertTrue(ig.addToGrid(sw));

        assertEquals("ne", ig.getIslandStringAt(150, 150));
        assertEquals("nw", ig.getIslandStringAt(-150, 150));
        assertEquals("se", ig.getIslandStringAt(150, -150));
        assertEquals("sw", ig.getIslandStringAt(-150, -150));

        // Outside all islands
        assertNull(ig.getIslandStringAt(0, 0));
    }

    /**
     * Tests mix of small and large range islands.
     */
    @Test
    void testVariableRangeIslands() {
        // Large island: range=500, covers [0, 1000) x [0, 1000)
        Island large = createMockIsland("large", 0, 0, 500);
        when(im.getIslandById("large")).thenReturn(large);
        assertTrue(ig.addToGrid(large));

        // Small island outside: range=10, at (1500, 1500)
        Island smallOutside = createMockIsland("small-out", 1500, 1500, 10);
        when(im.getIslandById("small-out")).thenReturn(smallOutside);
        assertTrue(ig.addToGrid(smallOutside));

        // Small island overlapping large: should be rejected
        Island smallInside = createMockIsland("small-in", 500, 500, 10);
        assertFalse(ig.addToGrid(smallInside));

        assertEquals("large", ig.getIslandStringAt(999, 999));
        assertNull(ig.getIslandStringAt(1000, 1000)); // just outside large
        assertEquals("small-out", ig.getIslandStringAt(1510, 1510));
    }

    /**
     * Tests that an island straddling a cell boundary is found from both sides.
     */
    @Test
    void testCellBoundaryLookup() {
        // Place island so it straddles the cell boundary at x=256
        // minX=200, range=50 -> covers [200, 300) which crosses cell boundary at 256
        Island crossingIsland = createMockIsland("crossing", 200, 0, 50);
        when(im.getIslandById("crossing")).thenReturn(crossingIsland);
        assertTrue(ig.addToGrid(crossingIsland));

        // Query on both sides of the cell boundary
        assertEquals("crossing", ig.getIslandStringAt(210, 10)); // cell 0
        assertEquals("crossing", ig.getIslandStringAt(270, 10)); // cell 1
        assertEquals("crossing", ig.getIslandStringAt(299, 99)); // near edge, cell 1
        assertNull(ig.getIslandStringAt(300, 10)); // just outside
    }

    /**
     * Tests that removal clears island from all cells.
     */
    @Test
    void testRemoveThenRequery() {
        Island crossingIsland = createMockIsland("crossing", 200, 0, 50);
        when(im.getIslandById("crossing")).thenReturn(crossingIsland);
        assertTrue(ig.addToGrid(crossingIsland));
        assertEquals("crossing", ig.getIslandStringAt(270, 10));

        assertTrue(ig.removeFromGrid(crossingIsland));
        assertNull(ig.getIslandStringAt(210, 10));
        assertNull(ig.getIslandStringAt(270, 10));
        assertEquals(0, ig.getSize());
    }

    /**
     * Tests getIslandsInBounds returns correct results.
     */
    @Test
    void testGetIslandsInBounds() {
        Island i1 = createMockIsland("i1", 0, 0, 50);
        Island i2 = createMockIsland("i2", 200, 200, 50);
        Island i3 = createMockIsland("i3", 1000, 1000, 50);

        ig.addToGrid(i1);
        ig.addToGrid(i2);
        ig.addToGrid(i3);

        // Query a region that covers i1 and i2 but not i3
        Collection<IslandData> result = ig.getIslandsInBounds(0, 0, 400, 400);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> d.id().equals("i1")));
        assertTrue(result.stream().anyMatch(d -> d.id().equals("i2")));

        // Query that covers only i3
        Collection<IslandData> result2 = ig.getIslandsInBounds(900, 900, 1200, 1200);
        assertEquals(1, result2.size());
        assertEquals("i3", result2.iterator().next().id());

        // Query that covers nothing
        Collection<IslandData> result3 = ig.getIslandsInBounds(5000, 5000, 6000, 6000);
        assertTrue(result3.isEmpty());
    }

    /**
     * Tests many non-overlapping islands packed tightly.
     */
    @Test
    void testDenseCluster() {
        // 10x10 grid of islands, each range=5 (diameter=10), spaced 10 apart (touching edges)
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                String id = "dense-" + x + "-" + z;
                Island di = createMockIsland(id, x * 10, z * 10, 5);
                when(im.getIslandById(id)).thenReturn(di);
                assertTrue(ig.addToGrid(di), "Should add " + id);
            }
        }
        assertEquals(100, ig.getSize());

        // Check a few
        assertEquals("dense-0-0", ig.getIslandStringAt(5, 5));
        assertEquals("dense-9-9", ig.getIslandStringAt(95, 95));
        assertEquals("dense-5-3", ig.getIslandStringAt(55, 35));

        // Just outside the cluster
        assertNull(ig.getIslandStringAt(100, 50));
    }

    // ---- Helper ----

    private Island createMockIsland(String id, int minX, int minZ, int range) {
        Island mock = org.mockito.Mockito.mock(Island.class);
        when(mock.getMinX()).thenReturn(minX);
        when(mock.getMinZ()).thenReturn(minZ);
        when(mock.getRange()).thenReturn(range);
        when(mock.getUniqueId()).thenReturn(id);
        return mock;
    }

}
