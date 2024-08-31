package world.bentobox.bentobox.util;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ExpiringMapTest {

    /**
     * Test method for {@link world.bentobox.bentobox.util.ExpiringMap#ExpiringMap(long, java.util.concurrent.TimeUnit)}.
     * @throws InterruptedException 
     */
    @Test
    public void testExpiringMap() throws InterruptedException {
        ExpiringMap<String, String> expiringMap = new ExpiringMap<>(5, TimeUnit.SECONDS);

        expiringMap.put("key1", "value1");
        assertEquals(1, expiringMap.size());

        // Check if key1 is present
        assertTrue(expiringMap.containsKey("key1"));

        // Using computeIfAbsent
        String value = expiringMap.computeIfAbsent("key2", k -> "computedValue");
        assertEquals("computedValue", value);
        assertEquals(2, expiringMap.size());

        // Check if key2 is present
        assertTrue(expiringMap.containsKey("key2"));

        // Use Awaitility to wait for keys to expire
        await().atMost(Duration.ofSeconds(6))
                .until(() -> !expiringMap.containsKey("key1") && !expiringMap.containsKey("key2"));

        assertFalse(expiringMap.containsKey("key1"));
        assertFalse(expiringMap.containsKey("key2"));
        assertTrue(expiringMap.isEmpty());

        expiringMap.shutdown();
    }

}
