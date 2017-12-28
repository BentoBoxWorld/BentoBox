package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.AddOn;

public class AddOnDisableEvent extends PremadeEvent{

	/**
	 * @author ComminQ_Q
	 * 			
	 * 			Event active when a addon is being disabled
	 * 			// TODO You can disable addon IG
	 * 
	 */
	
	private AddOn addon;

	public AddOnDisableEvent(AddOn addon){
		this.addon = addon;
	}
	
	public AddOn getAddon() {
		return addon;
	}

	public void setAddon(AddOn addon) {
		this.addon = addon;
	}
	
}
