package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExpiringSet} equals and hashCode.
 */
class ExpiringSetTest {

    private ExpiringSet<String> set;

    @BeforeEach
    void setUp() {
        set = new ExpiringSet<>(1, TimeUnit.HOURS);
    }

    @AfterEach
    void tearDown() {
        set.close();
    }

    @Test
    void testEqualsReflexive() {
        set.add("a");
        assertEquals(set, set);
    }

    @Test
    void testEqualsEmptySets() {
        try (ExpiringSet<String> other = new ExpiringSet<>(1, TimeUnit.HOURS)) {
            assertEquals(set, other);
            assertEquals(other, set);
        }
    }

    @Test
    void testEqualsSameElements() {
        set.add("a");
        set.add("b");

        try (ExpiringSet<String> other = new ExpiringSet<>(1, TimeUnit.HOURS)) {
            other.add("a");
            other.add("b");
            assertEquals(set, other);
            assertEquals(other, set);
        }
    }

    @Test
    void testEqualsWithHashSet() {
        set.add("a");
        set.add("b");

        Set<String> hashSet = new HashSet<>();
        hashSet.add("a");
        hashSet.add("b");

        assertEquals(set, hashSet);
    }

    @Test
    void testNotEqualsDifferentElements() {
        set.add("a");

        try (ExpiringSet<String> other = new ExpiringSet<>(1, TimeUnit.HOURS)) {
            other.add("b");
            assertNotEquals(set, other);
        }
    }

    @Test
    void testNotEqualsNull() {
        assertNotEquals(null, set);
    }

    @Test
    void testNotEqualsNonSet() {
        assertFalse(set.equals("not a set"));
    }

    @Test
    void testHashCodeConsistentWithEquals() {
        set.add("x");
        set.add("y");

        try (ExpiringSet<String> other = new ExpiringSet<>(1, TimeUnit.HOURS)) {
            other.add("x");
            other.add("y");
            assertEquals(set, other);
            assertEquals(set.hashCode(), other.hashCode());
        }
    }

    @Test
    void testHashCodeMatchesHashSet() {
        set.add("a");
        set.add("b");

        Set<String> hashSet = new HashSet<>();
        hashSet.add("a");
        hashSet.add("b");

        assertEquals(hashSet.hashCode(), set.hashCode());
    }

    @Test
    void testEmptySetHashCode() {
        assertEquals(new HashSet<>().hashCode(), set.hashCode());
    }
}
