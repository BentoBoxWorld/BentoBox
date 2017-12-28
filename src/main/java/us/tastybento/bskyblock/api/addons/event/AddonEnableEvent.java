package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.Addon;

/**
 * This event is run when an addon is getting enabled.
 *
 * @author ComminQ
 */
public class AddonEnableEvent extends PremadeEvent {
	private Addon addon;

	public AddonEnableEvent(Addon addon){
		this.addon = addon;
	}
	
	public Addon getAddon() {
		return addon;
	}

	public void setAddon(Addon addon) {
		this.addon = addon;
	}
	
}
