package us.tastybento.bskyblock.api.configuration;


public interface Adapter<S,V> {

    /**
     * Convert from to something
     * @param from
     */
     S convertFrom(Object from);
     
     V convertTo(Object to);
}
