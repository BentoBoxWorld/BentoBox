package world.bentobox.bentobox.api.events;

import org.bukkit.event.Event;

/**
 * Provides the default methods expected when extending {@link Event}.
 * @deprecated Use {@link BentoBoxEvent} instead
 */
@Deprecated
public abstract class PremadeEvent extends BentoBoxEvent {

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

}
