package world.bentobox.bentobox.api.events;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Provides the default methods expected when extending {@link Event}.
 */
public abstract class PremadeEvent extends Event {

    /**
     * The default constructor is defined for cleaner code.
     * This constructor assumes the PremadeEvent is synchronous.
     */
    public PremadeEvent() {
        this(false);
    }

    /**
     * This constructor is used to explicitly declare an PremadeEvent as synchronous or asynchronous.
     * @param async - true indicates the event will fire asynchronously, false
     *    by default from default constructor
     * @since 1.5.2
     */
    public PremadeEvent(boolean async) {
        super(async);
    }

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Get a map of key value pairs derived from the fields of this class by reflection.
     * @return map
     * @since 1.5.3
     */
    public Map<String, Object> getKeyValues() {
        try {
            Map<String, Object> map = new HashMap<>();
            Arrays.asList(Introspector.getBeanInfo(this.getClass(), PremadeEvent.class).getPropertyDescriptors())
            .stream()
            // only get getters
            .filter(pd -> Objects.nonNull(pd.getReadMethod()))
            .forEach(pd -> { // invoke method to get value
                try {
                    Object value = pd.getReadMethod().invoke(this);
                    if (value != null) {
                        map.put(pd.getName(), value);
                    }
                } catch (Exception ignore) {}
            });
            return map;
        } catch (IntrospectionException e) {
            // Oh well, nothing
            return Collections.emptyMap();
        }
    }

}
