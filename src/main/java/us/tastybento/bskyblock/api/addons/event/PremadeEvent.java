package us.tastybento.bskyblock.api.addons.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class PremadeEvent extends Event{

	public static final HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandleList(){
		return handlers;
	}
	
}
