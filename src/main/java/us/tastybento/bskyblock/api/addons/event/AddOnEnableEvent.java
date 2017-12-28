package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.AddOn;

public class AddOnEnableEvent extends PremadeEvent{

	/**
	 * @author ComminQ_Q
	 * 			
	 * 			Event active when a addon is being disabled
	 * 			// TODO You can enable Addon IG
	 * 
	 */
	
	private AddOn addon;

	public AddOnEnableEvent(AddOn addon){
		this.addon = addon;
	}
	
	public AddOn getAddon() {
		return addon;
	}

	public void setAddon(AddOn addon) {
		this.addon = addon;
	}
	
}
