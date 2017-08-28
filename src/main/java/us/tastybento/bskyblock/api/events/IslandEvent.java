package us.tastybento.bskyblock.api.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.bskyblock.database.objects.Island;

/**
 *
 * @author Poslovitch
 * @version 1.0
 */
public class IslandEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final Island island;

    /**
     * @param island
     */
    public IslandEvent(Island island){
        this.island = island;
    }

    /**
     * @return the island involved in this event
     */
    public Island getIsland(){
        return this.island;
    }
    
    /**
     * @return the owner of the island
     */
    public UUID getOwner() {
        return this.getOwner();
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
