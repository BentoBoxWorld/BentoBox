package world.bentobox.bentobox.api.events.addon;

import java.util.Map;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.PremadeEvent;

/**
 * @author Poslovitch
 * @since 1.0
 */
public class AddonBaseEvent extends PremadeEvent {

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
    public Map<String, Object> getKeyValues() {
        return keyValues;
    }
}
