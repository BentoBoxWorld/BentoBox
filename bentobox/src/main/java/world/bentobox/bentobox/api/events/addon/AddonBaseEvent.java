package world.bentobox.bentobox.api.events.addon;

import java.util.Map;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.BentoBoxEvent;

/**
 * @author Poslovitch
 */
public class AddonBaseEvent extends BentoBoxEvent {

    private final Addon addon;
    private final Map<String, Object> keyValues;

    public AddonBaseEvent(Addon addon, Map<String, Object> keyValues) {
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
}
