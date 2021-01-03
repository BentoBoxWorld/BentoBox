package world.bentobox.bentobox.api.metadata;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This interface is for all BentoBox objects that have meta data
 * @author tastybento
 * @since 1.15.4
 */
public interface MetaDataAble {
    /**
     * @return the metaData, may be null
     */
    @Nullable
    public Map<String, MetaDataValue> getMetaData();

    /**
     * Get meta data by key
     * @param key - key
     * @return the value to which the specified key is mapped, or null if there is no mapping for the key
     * @since 1.15.4
     */
    @Nullable
    public MetaDataValue getMetaData(@NonNull String key);

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     */
    public void setMetaData(Map<String, MetaDataValue> metaData);

    /**
     * Put a key, value string pair into the object's meta data
     * @param key - key
     * @param value - value
     * @return the previous value associated with key, or null if there was no mapping for key.
     * @since 1.15.4
     */
    @Nullable
    public MetaDataValue putMetaData(@NonNull String key, @NonNull MetaDataValue value);

    /**
     * Remove meta data
     * @param key - key to remove
     * @return the previous value associated with key, or null if there was no mapping for key.
     * @since 1.15.4
     */
    @Nullable
    public MetaDataValue removeMetaData(@NonNull String key);
}
