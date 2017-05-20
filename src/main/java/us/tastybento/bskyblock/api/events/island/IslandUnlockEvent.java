package us.tastybento.bskyblock.api.events.island;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired when an island is going to be unlocked.
 * <p>
 * Cancelling this event will result in keeping the island locked.
 * @author Poslovitch
 * @since 4.0
 */
public class IslandUnlockEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private final Island island;
	private boolean cancelled;
	
	/**
	 * @param island
	 */
	public IslandUnlockEvent(Island island){
		this.island = island;
	}
	
	/**
	 * @return the locked island
	 */
	public Island getIsland(){
		return this.island;
	}
	
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
	
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
