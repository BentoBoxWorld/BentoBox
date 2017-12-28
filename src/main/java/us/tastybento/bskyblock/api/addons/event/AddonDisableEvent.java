package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.Addon;

/**
 * This event is run when an addon is getting disabled.
 *
 * @author ComminQ
 */
public class AddonDisableEvent extends PremadeEvent {
	private Addon addon;

	public AddonDisableEvent(Addon addon){
		this.addon = addon;
	}
	
	public Addon getAddon() {
		return addon;
	}

	public void setAddon(Addon addon) {
		this.addon = addon;
	}
	
}
