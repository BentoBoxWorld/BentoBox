package us.tastybento.bskyblock.api.configuration;


/**
 * Convert from to S or to V
 * @author tastybento
 *
 * @param <S>
 * @param <V>
 */
public interface Adapter<S,V> {

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
