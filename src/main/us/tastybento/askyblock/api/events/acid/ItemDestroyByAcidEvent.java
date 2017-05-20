package us.tastybento.askyblock.api.events.acid;

import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.askyblock.database.objects.Island;

/**
 * Fired when an item (on the ground) gets destroyed by acid
 * @author Poslovitch
 * @since 4.0
 */
public class ItemDestroyByAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Item item;
    
    public ItemDestroyByAcidEvent(Island island, Item item) {
        this.island = island;
        this.item = item;
    }
    
    /**
     * Gets the island where stands the destroyed item
     * @return the island where stands the destroyed item
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the item which is getting destroyed by Acid
     * @return the destroyed item
     */
    public Item getItem(){
        return item;
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
