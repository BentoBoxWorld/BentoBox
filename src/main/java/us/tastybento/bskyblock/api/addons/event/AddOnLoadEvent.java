package us.tastybento.bskyblock.api.addons.event;

import us.tastybento.bskyblock.api.addons.AddOn;

public class AddOnLoadEvent extends PremadeEvent{

	private AddOn addon;

	public AddOnLoadEvent(AddOn addon){
		this.addon = addon;
	}
	
	public AddOn getAddOn() {
		return addon;
	}

	public void setAddOn(AddOn addon) {
		this.addon = addon;
	}
	
}
