package world.bentobox.bentobox.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Provides the default methods expected when extending {@link Event}.
 */
public abstract class PremadeEvent extends Event {

    /**
     * The default constructor is defined for cleaner code. This constructor
     * assumes the PremadeEvent is synchronous.
     */
    public PremadeEvent()
    {
        this(false);
    }


    /**
     * This constructor is used to explicitly declare an PremadeEvent as synchronous
     * or asynchronous.
     * @param async - true indicates the event will fire asynchronously, false
     *    by default from default constructor
     */
    public PremadeEvent(boolean async)
    {
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
}
