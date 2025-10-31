package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.database.objects.Island;

/**
 * Grid test
 */
@RunWith(PowerMockRunner.class)
public class IslandGridTest {
    
    private IslandGrid ig;
    @Mock
    private IslandCache im;
    @Mock
    private Island island;
    @Mock
    private Island island2;
    @Mock
    private Island overlappingIsland;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
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
    @After
    public void tearDown() throws Exception {
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

}
