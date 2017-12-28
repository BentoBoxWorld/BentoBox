package us.tastybento.bskyblock.api.events.addon;

import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.events.PremadeEvent;

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
