package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExpiringMap} equals and hashCode.
 */
class ExpiringMapTest {

    private ExpiringMap<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new ExpiringMap<>(1, TimeUnit.HOURS);
    }

    @AfterEach
    void tearDown() {
        map.shutdown();
    }

    @Test
    void testEqualsReflexive() {
        map.put("a", 1);
        assertEquals(map, map);
    }

    @Test
    void testEqualsEmptyMaps() {
        ExpiringMap<String, Integer> other = new ExpiringMap<>(1, TimeUnit.HOURS);
        try {
            assertEquals(map, other);
            assertEquals(other, map);
        } finally {
            other.shutdown();
        }
    }

    @Test
    void testEqualsSameEntries() {
        map.put("a", 1);
        map.put("b", 2);

        ExpiringMap<String, Integer> other = new ExpiringMap<>(1, TimeUnit.HOURS);
        other.put("a", 1);
        other.put("b", 2);

        try {
            assertEquals(map, other);
            assertEquals(other, map);
        } finally {
            other.shutdown();
        }
    }

    @Test
    void testEqualsWithHashMap() {
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);

        assertEquals(map, hashMap);
    }

    @Test
    void testNotEqualsDifferentEntries() {
        map.put("a", 1);

        ExpiringMap<String, Integer> other = new ExpiringMap<>(1, TimeUnit.HOURS);
        other.put("b", 2);

        try {
            assertNotEquals(map, other);
        } finally {
            other.shutdown();
        }
    }

    @Test
    void testNotEqualsDifferentValues() {
        map.put("a", 1);

        ExpiringMap<String, Integer> other = new ExpiringMap<>(1, TimeUnit.HOURS);
        other.put("a", 99);

        try {
            assertNotEquals(map, other);
        } finally {
            other.shutdown();
        }
    }

    @Test
    void testNotEqualsNull() {
        assertNotEquals(null, map);
    }

    @Test
    void testNotEqualsNonMap() {
        assertNotEquals("not a map", map);
    }

    @Test
    void testHashCodeConsistentWithEquals() {
        map.put("x", 10);
        map.put("y", 20);

        ExpiringMap<String, Integer> other = new ExpiringMap<>(1, TimeUnit.HOURS);
        other.put("x", 10);
        other.put("y", 20);

        try {
            assertEquals(map, other);
            assertEquals(map.hashCode(), other.hashCode());
        } finally {
            other.shutdown();
        }
    }

    @Test
    void testHashCodeMatchesHashMap() {
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);

        assertEquals(hashMap.hashCode(), map.hashCode());
    }

    @Test
    void testEmptyMapHashCode() {
        assertEquals(new HashMap<>().hashCode(), map.hashCode());
    }
}
