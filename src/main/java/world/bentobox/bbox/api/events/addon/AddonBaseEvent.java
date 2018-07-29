package world.bentobox.bbox.api.events.addon;

import world.bentobox.bbox.api.addons.Addon;
import world.bentobox.bbox.api.events.PremadeEvent;

/**
 * @author Poslovitch
 * @since 1.0
 */
public class AddonBaseEvent extends PremadeEvent {

    private final Addon addon;

    public AddonBaseEvent(Addon addon) {
        super();
        this.addon = addon;
    }

    public Addon getAddon() {
        return addon;
    }
}
