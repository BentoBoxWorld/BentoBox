package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.Addon;

/**
 * This event is run when an addon is getting loaded.
 *
 * @author ComminQ
 */
public class AddonLoadEvent extends PremadeEvent {
	private Addon addon;

	public AddonLoadEvent(Addon addon){
		this.addon = addon;
	}

	public Addon getAddon() {
		return addon;
	}

	public void setAddon(Addon addon) {
		this.addon = addon;
	}
	
}
