package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        assertNotEquals("not a set", set);
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

    @Test
    void expiredEntryDisappearsWithoutScheduler() throws InterruptedException {
        try (var shortLived = new ExpiringSet<String>(10, TimeUnit.MILLISECONDS)) {
            shortLived.add("a");
            Thread.sleep(40);
            assertFalse(shortLived.contains("a"));
            assertEquals(0, shortLived.size());
        }
    }

    @Test
    void closeClearsAndRejectsNewEntries() {
        set.add("a");
        set.close();
        assertEquals(0, set.size());
        org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.RejectedExecutionException.class,
                () -> set.add("b"));
    }

    @Test
    void iteratorRemovalAndBulkOperationsRetainSetSemantics() {
        set.addAll(java.util.List.of("a", "b", "c"));
        var iterator = set.iterator();
        String removed = iterator.next();
        iterator.remove();
        assertFalse(set.contains(removed));
        set.retainAll(Set.of("a"));
        assertEquals(set.contains("a") ? Set.of("a") : Set.of(), set);
        set.clear();
        assertEquals(0, set.size());
    }
}
