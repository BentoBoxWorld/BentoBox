package us.tastybento.askyblock.api.events.acid;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.askyblock.database.objects.Island;

/**
 * Fired when a player drinks acid and... DIES
 * @author Poslovitch
 * @since 4.0
 */
public class PlayerDrinkAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Player player;
    
    public PlayerDrinkAcidEvent(Island island, Player player) {
        this.island = island;
        this.player = player;
    }
    
    /**
     * Gets the island where stands the killed player
     * @return the island where stands the killed player
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the player which is getting killed by its stupid thirsty
     * @return the killed player
     */
    public Player getPlayer(){
        return player;
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
