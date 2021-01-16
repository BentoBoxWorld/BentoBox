package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PairTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public final void testHashCode() {
        Pair<Integer, Integer> pair = new Pair<>(1,2);
        Pair<Integer, Integer> pair2 = new Pair<>(1,2);
        assertEquals(pair.hashCode(), pair2.hashCode());
    }

    @Test
    public final void testPair() {
        Pair<Integer, Integer> pair = new Pair<>(1,2);
        assertEquals(Integer.valueOf(1), pair.x);
        assertEquals(Integer.valueOf(2), pair.z);
    }

    @Test
    public final void testToString() {
        Pair<Integer, Integer> pair = new Pair<>(1,2);
        assertEquals("Pair [x=1, z=2]", pair.toString());
    }

    @Test
    public final void testEqualsObject() {
        Pair<Integer, Integer> pair = new Pair<>(1,2);
        Pair<String, String> pair2 = new Pair<>("1","2");
        Pair<Integer, Integer> pair3 = new Pair<>(1,2);
        Pair<Integer, Integer> pair4 = new Pair<>(1,null);
        Pair<Integer, Integer> pair5 = new Pair<>(null,2);
        assertEquals(pair, pair);
        assertTrue(pair.equals(pair3) && pair3.equals(pair));
        assertNotEquals(pair, pair2);
        assertNotEquals(pair, pair4);
        assertNotEquals(pair, pair5);
    }

}
