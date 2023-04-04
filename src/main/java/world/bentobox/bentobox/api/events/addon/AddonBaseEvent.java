package world.bentobox.bentobox.api.events.addon;

import java.util.Map;
import java.util.Optional;

import org.bukkit.event.HandlerList;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.BentoBoxEvent;

/**
 * Base abstract class for addon events
 * @author Poslovitch
 */
public abstract class AddonBaseEvent extends BentoBoxEvent {

    protected static final HandlerList handlers = new HandlerList();
    private final Addon addon;
    private final Map<String, Object> keyValues;
    private AddonBaseEvent newEvent;

    protected AddonBaseEvent(Addon addon, Map<String, Object> keyValues) {
        super();
        this.addon = addon;
        this.keyValues = keyValues;
    }

    public Addon getAddon() {
        return addon;
    }

    /**
     * @return the keyValues
     */
    @Override
    public Map<String, Object> getKeyValues() {
        return keyValues;
    }

    /**
     * Get new event if this event is deprecated
     * @return optional newEvent or empty if there is none
     */
    public Optional<AddonBaseEvent> getNewEvent() {
        return Optional.ofNullable(newEvent);
    }

    /**
     * Set the newer event so it can be obtained if this event is deprecated
     * @param newEvent the newEvent to set
     */
    public void setNewEvent(AddonBaseEvent newEvent) {
        this.newEvent = newEvent;
    }


}
