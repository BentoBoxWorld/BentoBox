package world.bentobox.bentobox.api.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * This interface is for all BentoBox objects that have metadata
 * @author tastybento
 * @since 1.15.6
 */
public interface MetaDataAble {

    /**
     * @return the metaData
     */
    Optional<Map<String, MetaDataValue>> getMetaData();

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     */
    void setMetaData(Map<String, MetaDataValue> metaData);

    /**
     * Get metadata by key
     * @param key - key
     * @return the value to which the specified key is mapped, or null if there is no mapping for the key
     * @since 1.15.6
     */
    default Optional<MetaDataValue> getMetaData(String key) {
        return getMetaData().map(m -> m.get(key));
    }

    /**
     * Put a key, value string pair into the metadata. A null value removes the key, because the
     * backing map does not store nulls.
     * @param key - key
     * @param value - value, or null to remove the key
     * @return the previous value associated with key, or empty if there was no mapping for key.
     * @since 1.15.6
     */
    default Optional<MetaDataValue> putMetaData(String key, MetaDataValue value) {
        if (value == null) {
            return removeMetaData(key);
        }
        return getMetaData().map(m -> m.put(key, value));
    }

    /**
     * Remove meta data
     * @param key - key to remove
     * @return the previous value associated with key, or empty if there was no mapping for key.
     * @since 1.15.6
     */
    default Optional<MetaDataValue> removeMetaData(String key) {
        return getMetaData().map(m -> m.remove(key));
    }

    /**
     * Copies the given map into a new {@link ConcurrentHashMap}, dropping null keys and values,
     * which a concurrent map cannot hold. Implementations use this to make their backing map
     * safe for concurrent access without mutating the caller's map.
     * @param source map to copy, may be null
     * @return a mutable, thread-safe copy
     * @since 3.23.1
     */
    static Map<String, MetaDataValue> toConcurrentMap(Map<String, MetaDataValue> source) {
        Map<String, MetaDataValue> result = new ConcurrentHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

}
