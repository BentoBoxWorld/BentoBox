package world.bentobox.bentobox.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

public class ExpiringSetTest {

    // Helper method to wait for expiration with a safety margin
    private void waitForExpiration(long expirationTimeMillis) {
        try {
            // Wait slightly longer than the expiration time to ensure scheduler runs
            Thread.sleep(expirationTimeMillis + 20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Since ExpiringSet implements AutoCloseable, we use try-with-resources in tests.
     * The @AfterEach method is no longer strictly necessary but kept for standard test structure.
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testExpiringSetConstructorValid() {
        // Test with valid positive time using try-with-resources
        try (ExpiringSet<String> set = new ExpiringSet<>(100, TimeUnit.MILLISECONDS)) {
            assertNotNull(set);
            assertTrue(set.isEmpty());
        }
    }

    /**
     * Fix for resource leak warning: The try-with-resources syntax cannot be used when 
     * the constructor itself is expected to throw an exception, as the resource is never
     * assigned. We use an internal try-catch block here instead of @Test(expected).
     */
    @Test
    public void testExpiringSetConstructorZeroTime() {
        try {
            new ExpiringSet<>(0, TimeUnit.MILLISECONDS);
            fail("Expected IllegalArgumentException was not thrown for zero time.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * Fix for resource leak warning.
     */
    @Test
    public void testExpiringSetConstructorNegativeTime() {
        try {
            new ExpiringSet<>(-10, TimeUnit.SECONDS);
            fail("Expected IllegalArgumentException was not thrown for negative time.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * Fix for resource leak warning.
     */
    @Test
    public void testExpiringSetConstructorNullTimeUnit() {
        try {
            new ExpiringSet<>(10, null);
            fail("Expected NullPointerException was not thrown for null TimeUnit.");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testShutdown() {
        // Use a longer expiration time for more reliable time-based testing
        final long expiration = 200; 

        // Explicitly create set outside try-with-resources to test manual shutdown
        ExpiringSet<String> set = new ExpiringSet<>(expiration, TimeUnit.MILLISECONDS);
        String element = "test";
        
        try { 
            set.add(element);
            // Immediately shut down (testing manual shutdown)
            set.shutdownNow();

            // Wait for the scheduled removal time (which should now be blocked)
            waitForExpiration(expiration);

            // Verify the element was NOT removed because the scheduler was shut down
            assertTrue("Element should NOT have been removed after shutdown and waiting for expiration.", set.contains(element));
            
            // Clean up: manually remove for clean test state
            set.remove(element);
            assertTrue(set.isEmpty());
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted unexpectedly.");
        } finally {
            // Call close() just in case the manual shutdown failed, relying on close() being idempotent.
            set.close();
        }
    }

    @Test
    public void testAddAndExpiration() {
        final long expiration = 50;
        try (ExpiringSet<String> set = new ExpiringSet<>(expiration, TimeUnit.MILLISECONDS)) {
            String element = "A";
            
            // 1. Add element
            assertTrue(set.add(element));
            assertEquals(1, set.size());
            assertTrue(set.contains(element));

            // 2. Wait for expiration
            waitForExpiration(expiration);

            // 3. Verify element is removed
            assertEquals(0, set.size());
            assertFalse(set.contains(element));

            // 4. Test adding the same element again
            assertTrue(set.add(element));
            assertEquals(1, set.size());
        }
    }

    @Test
    public void testAddExpirationRefresh() {
        final long expiration = 200;
        try (ExpiringSet<String> set = new ExpiringSet<>(expiration, TimeUnit.MILLISECONDS)) {
            String element = "refreshed";

            // Add element (T=0)
            set.add(element);

            // Wait for half the time (T=50)
            waitForExpiration(expiration / 2);
            assertTrue("Element should still be present before expiration", set.contains(element));

            // Add element again, refreshing its timer (T=50, new expiration at T=150)
            assertFalse("Adding an existing element should return false", set.add(element));

            // Wait for the original expiration time (T=100)
            waitForExpiration(expiration / 2);
            assertTrue("Element should still be present because timer was refreshed", set.contains(element));

            // Wait for the new expiration time (T=150)
            waitForExpiration(expiration);
            assertFalse("Element should be removed after the refreshed timer expires", set.contains(element));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullElement() {
        try (ExpiringSet<String> set = new ExpiringSet<>(1, TimeUnit.SECONDS)) {
            set.add(null);
        }
    }

    @Test
    public void testSize() {
        try (ExpiringSet<Integer> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            assertEquals(0, set.size());
            set.add(1);
            assertEquals(1, set.size());
            set.add(2);
            assertEquals(2, set.size());
            set.add(1); // Adding duplicate
            assertEquals(2, set.size());
            set.remove(2);
            assertEquals(1, set.size());
        }
    }

    @Test
    public void testIsEmpty() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            assertTrue(set.isEmpty());
            set.add("A");
            assertFalse(set.isEmpty());
            set.remove("A");
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void testContains() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            String element = "CheckMe";
            set.add(element);
            assertTrue(set.contains(element));
            assertFalse(set.contains("NotPresent"));
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testContainsNull() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.contains(null);
        }
    }

    @Test
    public void testRemove() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.add("A");
            set.add("B");
            
            assertTrue(set.remove("A"));
            assertFalse(set.contains("A"));
            assertEquals(1, set.size());

            assertFalse(set.remove("C")); // Removing non-existent
            assertEquals(1, set.size());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveNull() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.remove(null);
        }
    }

    @Test
    public void testContainsAll() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.add("A");
            set.add("B");
            set.add("C");
            
            Collection<String> subset = Arrays.asList("A", "C");
            assertTrue(set.containsAll(subset));
            
            Collection<String> superset = Arrays.asList("A", "C", "D");
            assertFalse(set.containsAll(superset));
            
            Collection<String> empty = new HashSet<>();
            assertTrue(set.containsAll(empty));
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testContainsAllNullCollection() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.containsAll(null);
        }
    }

    @Test
    public void testAddAll() {
        final long expiration = 500;
        try (ExpiringSet<Integer> set = new ExpiringSet<>(expiration, TimeUnit.MILLISECONDS)) {
            Collection<Integer> newElements = Arrays.asList(10, 20, 30);
            
            assertTrue(set.addAll(newElements));
            assertEquals(3, set.size());
            assertTrue(set.contains(20));
            
            // Check that adding a collection with duplicates works (only new elements are added)
            Collection<Integer> mixedElements = Arrays.asList(30, 40);
            assertTrue(set.addAll(mixedElements));
            assertEquals(4, set.size()); // Should only add 40
            
            // Wait for expiration for a moment (should not expire yet)
            waitForExpiration(expiration/2);
            assertEquals(4, set.size());
            
            // Check that expiration still works
            waitForExpiration(expiration/2);
            assertEquals(0, set.size());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullCollection() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(null);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllCollectionWithNullElement() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            Collection<String> invalid = Arrays.asList("A", null, "B");
            set.addAll(invalid);
        }
    }

    @Test
    public void testRetainAll() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("A", "B", "C", "D"));
            
            Collection<String> toRetain = Arrays.asList("B", "D", "Z");
            
            assertTrue(set.retainAll(toRetain)); // Should remove A and C
            assertEquals(2, set.size());
            assertTrue(set.contains("B"));
            assertTrue(set.contains("D"));
            assertFalse(set.contains("A"));
            
            assertFalse(set.retainAll(toRetain)); // No changes
            assertEquals(2, set.size());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAllNullCollection() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.add("A");
            set.retainAll(null);
        }
    }

    @Test
    public void testRemoveAll() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("A", "B", "C", "D"));
            
            Collection<String> toRemove = Arrays.asList("A", "C", "E");
            
            assertTrue(set.removeAll(toRemove)); // Should remove A and C
            assertEquals(2, set.size());
            assertTrue(set.contains("B"));
            assertTrue(set.contains("D"));
            
            assertFalse(set.removeAll(toRemove)); // No changes
            assertEquals(2, set.size());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAllNullCollection() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.add("A");
            set.removeAll(null);
        }
    }

    @Test
    public void testClear() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("A", "B", "C"));
            assertEquals(3, set.size());
            
            set.clear();
            
            assertEquals(0, set.size());
            assertTrue(set.isEmpty());
            assertFalse(set.contains("A"));
        }
    }

    @Test
    public void testIterator() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
            
            Iterator<String> it = set.iterator();
            int count = 0;
            while (it.hasNext()) {
                it.next();
                count++;
            }
            assertEquals(3, count);
            
            // Test iterator remove
            it = set.iterator();
            String removed = it.next();
            it.remove();
            
            assertEquals(2, set.size());
            assertFalse(set.contains(removed));
        }
    }

    @Test
    public void testToArray() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("X", "Y", "Z"));
            Object[] array = set.toArray();
            assertEquals(3, array.length);
            
            Set<Object> resultSet = new HashSet<>(Arrays.asList(array));
            assertTrue(resultSet.contains("X"));
            assertTrue(resultSet.contains("Y"));
            assertTrue(resultSet.contains("Z"));
        }
    }

    @Test
    public void testToArrayTArray() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            set.addAll(Arrays.asList("X", "Y", "Z"));
            String[] targetArray = new String[3];
            String[] result = set.toArray(targetArray);
            
            assertSame(targetArray, result); // Should use the provided array
            
            Set<String> resultSet = new HashSet<>(Arrays.asList(result));
            assertTrue(resultSet.contains("X"));
            assertTrue(resultSet.contains("Y"));
            assertTrue(resultSet.contains("Z"));
            
            // Test with smaller array
            String[] smallArray = new String[0];
            String[] newResult = set.toArray(smallArray);
            assertNotSame(smallArray, newResult); // Should allocate new array
            assertEquals(3, newResult.length);
        }
    }
    
    @Test
    public void testEqualsObject() {
        // Since set is AutoCloseable, we can declare both sets in the resource header
        try (ExpiringSet<String> set1 = new ExpiringSet<>(500, TimeUnit.MILLISECONDS);
             ExpiringSet<String> set2 = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            
            // Empty sets are equal
            assertTrue(set1.equals(set2));
            
            set1.add("A");
            set1.add("B");
            
            assertFalse(set1.equals(set2));
            
            set2.add("B");
            set2.add("A");
            
            // Equal content, regardless of expiration time or order
            assertTrue(set1.equals(set2));
            
            // Test against a standard HashSet with the same elements
            Set<String> standardSet = new HashSet<>(Arrays.asList("A", "B"));
            assertTrue(set1.equals(standardSet));
            assertTrue(standardSet.equals(set1));
        }
    }

    @Test
    public void testHashCode() {
        try (ExpiringSet<String> set = new ExpiringSet<>(500, TimeUnit.MILLISECONDS)) {
            Set<String> standardSet = new HashSet<>();
            
            // Empty sets have same hash code
            assertEquals(standardSet.hashCode(), set.hashCode());
            
            set.add("A");
            standardSet.add("A");
            assertEquals(standardSet.hashCode(), set.hashCode());
            
            set.add("B");
            standardSet.add("B");
            // Set hash code is based on contents, not expiration/implementation details
            assertEquals(standardSet.hashCode(), set.hashCode());
        }
    }
}
