package world.bentobox.bentobox.database.objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.api.metadata.MetaDataValue;

/**
 * Tests for {@link Players} metadata handling.
 */
class PlayersTest {

    @Test
    void testGetMetaDataLazyInit() {
        Players p = new Players();
        assertTrue(p.getMetaData().isPresent());
        assertTrue(p.getMetaData().get().isEmpty());
    }

    @Test
    void testGetMetaDataReturnsSameMapOnRepeatedCalls() {
        Players p = new Players();
        Map<String, MetaDataValue> first = p.getMetaData().get();
        first.put("key", new MetaDataValue("value"));
        assertSame(first, p.getMetaData().get());
        assertEquals("value", p.getMetaData("key").get().asString());
    }

    @Test
    void testSetMetaDataImmutableMapIsCopiedAndMutable() {
        Players p = new Players();
        p.setMetaData(Map.of("key", new MetaDataValue("value")));
        Map<String, MetaDataValue> meta = p.getMetaData().get();
        assertEquals("value", meta.get("key").asString());
        assertDoesNotThrow(() -> meta.put("other", new MetaDataValue(true)));
        assertTrue(p.getMetaData("other").get().asBoolean());
    }

    @Test
    void testSetMetaDataDoesNotMutateCallerMap() {
        Players p = new Players();
        Map<String, MetaDataValue> meta = new HashMap<>();
        p.setMetaData(meta);
        p.putMetaData("key", new MetaDataValue("value"));
        assertTrue(meta.isEmpty());
        assertEquals("value", p.getMetaData("key").get().asString());
    }

    @Test
    void testSetMetaDataDropsNullEntries() {
        Players p = new Players();
        Map<String, MetaDataValue> meta = new HashMap<>();
        meta.put("key", new MetaDataValue("value"));
        meta.put("nullValue", null);
        meta.put(null, new MetaDataValue("nullKey"));
        p.setMetaData(meta);
        assertEquals(1, p.getMetaData().get().size());
        assertEquals("value", p.getMetaData("key").get().asString());
    }

    @Test
    void testSetMetaDataNullClears() {
        Players p = new Players();
        p.putMetaData("key", new MetaDataValue("value"));
        p.setMetaData(null);
        assertTrue(p.getMetaData().get().isEmpty());
    }

    @Test
    void testPutMetaDataNullValueRemovesKey() {
        Players p = new Players();
        p.putMetaData("key", new MetaDataValue("value"));
        assertEquals("value", p.putMetaData("key", null).get().asString());
        assertFalse(p.getMetaData("key").isPresent());
        assertFalse(p.putMetaData("missing", null).isPresent());
    }

    /**
     * Hammers the metadata map from several threads at once. Before the fix, a plain HashMap
     * could be corrupted by concurrent access and then throw NoSuchElementException on every
     * subsequent read.
     */
    @Test
    void testConcurrentMetaDataAccessDoesNotThrow() throws Exception {
        Players p = new Players();
        int threads = 8;
        int iterations = 5000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            java.util.List<Future<?>> futures = new java.util.ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int id = t;
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        String key = "key" + (i % 16) + "_" + id;
                        p.putMetaData(key, new MetaDataValue(i));
                        p.getMetaData("key" + (i % 16) + "_" + ((id + 1) % threads));
                        p.getMetaData().get().size();
                        if (i % 3 == 0) {
                            p.removeMetaData(key);
                        }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS); // rethrows any exception from the worker
            }
        } finally {
            pool.shutdownNow();
        }
        assertDoesNotThrow(() -> p.getMetaData("anything"));
    }
}
