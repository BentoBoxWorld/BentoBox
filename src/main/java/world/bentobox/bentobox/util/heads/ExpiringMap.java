package world.bentobox.bentobox.util.heads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ExpiringMap<K, V> {
    private final Map<K, V> map;
    private final ScheduledExecutorService scheduler;
    private final long expirationTime;

    public ExpiringMap(long expirationTime, TimeUnit timeUnit) {
        this.map = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.expirationTime = timeUnit.toMillis(expirationTime);
    }

    public void put(K key, V value) {
        map.put(key, value);
        scheduleRemoval(key);
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public V get(K key) {
        return map.get(key);
    }

    public V remove(K key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(k);
            scheduleRemoval(k);
            return value;
        });
    }

    private void scheduleRemoval(final K key) {
        scheduler.schedule(() -> map.remove(key), expirationTime, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
    /*
    public static void main(String[] args) throws InterruptedException {
        ExpiringMap<String, String> expiringMap = new ExpiringMap<>(5, TimeUnit.SECONDS);
    
        expiringMap.put("key1", "value1");
        System.out.println("Initial size: " + expiringMap.size()); // Should print 1
    
        // Using computeIfAbsent
        String value = expiringMap.computeIfAbsent("key2", k -> "computedValue");
        System.out.println("Computed value for key2: " + value); // Should print "computedValue"
        System.out.println("Size after computeIfAbsent: " + expiringMap.size()); // Should print 2
    
        Thread.sleep(6000);
        System.out.println("Size after 6 seconds: " + expiringMap.size()); // Should print 0
    
        expiringMap.shutdown();
    }*/
}
