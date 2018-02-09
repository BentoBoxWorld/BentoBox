package us.tastybento.bskyblock.database.objects.adapters;


/**
 * Convert from to S or to V
 * @author tastybento
 *
 * @param <S>
 * @param <V>
 */
public interface AdapterInterface<S,V> {

    /**
     * Serialize object
     * @param object - object
     * @return serialized object
     */
    S serialize(Object object);

    /**
     * Deserialize object
     * @param object
     * @return deserialized object
     */
    V deserialize(Object object);
}
