package world.bentobox.bentobox.database.objects.adapters;


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
     * @param object - object to serialize
     * @return serialized object
     */
    S deserialize(Object object);

    /**
     * Deserialize object
     * @param object - object to deserialize
     * @return deserialized object
     */
    V serialize(Object object);
}
