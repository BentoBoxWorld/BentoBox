package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.objects.Island;

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
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#addToGrid(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testAddToGrid() {
       assertTrue(ig.addToGrid(island));
       assertFalse(ig.addToGrid(overlappingIsland));
       assertTrue(ig.addToGrid(island2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#removeFromGrid(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testRemoveFromGrid() {
        assertTrue(ig.addToGrid(island));
       assertTrue(ig.removeFromGrid(island));
       assertFalse(ig.removeFromGrid(island2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getIslandAt(int, int)}.
     */
    @Test
    public void testGetIslandAt() {
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
    public void testIsIslandAt() {
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
    public void testGetIslandStringAt() {
       assertNull(ig.getIslandStringAt(0, 0));
       assertTrue(ig.addToGrid(island2));
       assertEquals("island2", ig.getIslandStringAt(0, 0));
       
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getSize()}.
     */
    @Test
    public void testGetSize() {
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
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandGrid#getGrid()}.
     */
    @Test
    public void testGetGrid() {
        assertNotNull(ig.getGrid());
    }
    
    @Test
    public void testUpdateIslandCoordinatesKeepsSingleEntry() {
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
    public void testAdjacentIslandsAllowedWhenEdgesTouch() {
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
    public void testLargeExistingIslandShouldBlockSmallIslandEvenIfMinXOutsideSubMapWindow() {
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
        // If this test fails, it reveals the current subMap window is too small to find big.
        assertFalse(ig.addToGrid(small), "Small island overlaps big island; should have been rejected");
    }

    @Test
    public void testGetIslandStringAtWhenXEntryExistsButNoZEntryApplies() {
        // island exists at minX=100 minZ=100 range=10 (covers z [110,110))
        when(zIsland.getMinX()).thenReturn(100);
        when(zIsland.getMinZ()).thenReturn(100);
        when(zIsland.getRange()).thenReturn(10);
        when(zIsland.getUniqueId()).thenReturn("z");

        when(im.getIslandById("z")).thenReturn(zIsland);

        assertTrue(ig.addToGrid(zIsland));

        // Query an x within island x-range but z is below any minZ -> should return null
        assertNull(ig.getIslandStringAt(110, 50));
    }

}
