package world.bentobox.bentobox.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A {@code ExpiringMap} is a map implementation that automatically removes entries after a
 * specified period of time. The expiration time is specified when the map is created and
 * applies to all entries put into the map. It is thread-safe and provides similar
 * functionality to {@code HashMap} with the added feature of automatic expiration of entries.
 *
 * <p>This class makes use of a {@link ConcurrentHashMap} for thread safety and a
 * {@link ScheduledExecutorService} to handle the expiration of entries. All operations are
 * thread-safe.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ExpiringMap<K, V> implements Map<K, V> {
    private final Map<K, V> map;
    private final ScheduledExecutorService scheduler;
    private final long expirationTime;

    /**
     * Constructs an empty {@code ExpiringMap} with the specified expiration time for entries.
     *
     * @param expirationTime the time after which entries should expire, in the specified time unit
     * @param timeUnit the time unit for the {@code expirationTime} parameter
     * @throws IllegalArgumentException if {@code expirationTime} is less than or equal to zero
     * @throws NullPointerException if {@code timeUnit} is null
     */
    public ExpiringMap(long expirationTime, TimeUnit timeUnit) {
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero.");
        }
        if (timeUnit == null) {
            throw new NullPointerException("TimeUnit cannot be null.");
        }
        this.map = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.expirationTime = timeUnit.toMillis(expirationTime);
    }

    /**
     * Associates the specified value with the specified key in this map. If the map
     * previously contained a mapping for the key, the old value is replaced.
     * The entry will automatically be removed after the specified expiration time.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if the specified key or value is null
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and Value cannot be null.");
        }
        V oldValue = map.put(key, value);
        scheduleRemoval(key);
        return oldValue;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null.");
        }
        return map.get(key);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map previously associated {@code null} with {@code key}.)
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null.");
        }
        return map.remove(key);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null.");
        }
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     * @throws NullPointerException if the specified value is null
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException("Value cannot be null.");
        }
        return map.containsValue(value);
    }

    /**
     * Returns the number of key-value mappings in this map. If the map contains more than
     * {@code Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Copies all of the mappings from the specified map to this map. The effect of this call is
     * equivalent to that of calling {@link #put(Object, Object) put(k, v)} on this map once
     * for each mapping from key {@code k} to value {@code v} in the specified map. The behavior
     * of this operation is undefined if the specified map is modified while the operation is in progress.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null, or if any key or value in the specified map is null
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) {
            throw new NullPointerException("The specified map cannot be null.");
        }
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map. The set is backed by the map,
     * so changes to the map are reflected in the set, and vice-versa. If the map is modified while
     * an iteration over the set is in progress, the results of the iteration are undefined. The set
     * supports element removal, which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
     * {@code clear} operations. It does not support the {@code add} or {@code addAll} operations.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map. The collection is
     * backed by the map, so changes to the map are reflected in the collection, and vice-versa.
     * If the map is modified while an iteration over the collection is in progress, the results
     * of the iteration are undefined. The collection supports element removal, which removes
     * the corresponding mapping from the map, via the {@code Iterator.remove}, {@code Collection.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear} operations. It does not support the
     * {@code add} or {@code addAll} operations.
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map. The set is backed by the map,
     * so changes to the map are reflected in the set, and vice-versa. If the map is modified while
     * an iteration over the set is in progress, the results of the iteration are undefined. The set
     * supports element removal, which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
     * {@code clear} operations. It does not support the {@code add} or {@code addAll} operations.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its
     * value using the given mapping function and enters it into this map unless {@code null}.
     *
     * <p>If the mapping function returns {@code null}, no mapping is recorded. If the mapping
     * function itself throws an (unchecked) exception, the exception is rethrown, and no mapping
     * is recorded. The computed value is set to expire after the specified expiration time.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key, or {@code null} if the computed value is {@code null}
     * @throws NullPointerException if the specified key or mappingFunction is null
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) {
            throw new NullPointerException("Key and mappingFunction cannot be null.");
        }
        return map.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(k);
            scheduleRemoval(k);
            return value;
        });
    }

    /**
     * Schedules the removal of the specified key from this map after the expiration time.
     *
     * @param key key whose mapping is to be removed from the map after the expiration time
     */
    private void scheduleRemoval(final K key) {
        scheduler.schedule(() -> map.remove(key), expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the {@code ScheduledExecutorService} used for scheduling the removal of
     * entries. This method should be called to release resources once the {@code ExpiringMap}
     * is no longer needed.
     *
     * <p>Once the executor is shut down, no more entries will be automatically removed.
     * It is the user's responsibility to ensure that the {@code shutdown} method is called.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

}