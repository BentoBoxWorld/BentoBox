package world.bentobox.bentobox.api.metadata;

import java.util.Map;
import java.util.Optional;

/**
 * This interface is for all BentoBox objects that have meta data
 * @author tastybento
 * @since 1.15.5
 */
public interface MetaDataAble {

    /**
     * @return the metaData
     */
    public Optional<Map<String, MetaDataValue>> getMetaData();

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     */
    public void setMetaData(Map<String, MetaDataValue> metaData);

    /**
     * Get meta data by key
     * @param key - key
     * @return the value to which the specified key is mapped, or null if there is no mapping for the key
     * @since 1.15.5
     */
    default Optional<MetaDataValue> getMetaData(String key) {
        return getMetaData().map(m -> m.get(key));
    }

    /**
     * Put a key, value string pair into the meta data
     * @param key - key
     * @param value - value
     * @return the previous value associated with key, or empty if there was no mapping for key.
     * @since 1.15.5
     */
    default Optional<MetaDataValue> putMetaData(String key, MetaDataValue value) {
        return getMetaData().map(m -> m.put(key, value));
    }

    /**
     * Remove meta data
     * @param key - key to remove
     * @return the previous value associated with key, or empty if there was no mapping for key.
     * @since 1.15.5
     */
    default Optional<MetaDataValue> removeMetaData(String key) {
        return getMetaData().map(m -> m.remove(key));
    }

}
