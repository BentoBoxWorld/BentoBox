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
     * Convert from to something
     * @param from
     */
     S convertFrom(Object from);
     
     V convertTo(Object to);
}
