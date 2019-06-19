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
 * @author tastybento
 * @since 1.5.3
 */
public abstract class BentoBoxEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The default constructor is defined for cleaner code.
     * This constructor assumes the BentoBoxEvent is synchronous.
     */
    public BentoBoxEvent() {
        this(false);
    }

    /**
     * Explicitly declares a BentoBoxEvent as synchronous or asynchronous.
     * @param async - true indicates the event will fire asynchronously, false
     *    by default from default constructor
     */
    public BentoBoxEvent(boolean async) {
        super(async);
    }

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
            Arrays.stream(Introspector.getBeanInfo(this.getClass(), BentoBoxEvent.class).getPropertyDescriptors())
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
