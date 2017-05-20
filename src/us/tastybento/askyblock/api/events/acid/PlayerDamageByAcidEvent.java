package us.tastybento.askyblock.api.events.acid;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.askyblock.database.objects.Island;

/**
 * Fired when a player receives damage from acid
 * @author Poslovitch
 * @since 4.0
 */
public class PlayerDamageByAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Player player;
    private double damage;
    
    public enum Acid { RAIN, WATER };
    private Acid cause;
    
    public PlayerDamageByAcidEvent(Island island, Player player, double damage, Acid cause) {
        this.island = island;
        this.player = player;
        this.damage = damage;
        this.cause = cause;
    }
    
    /**
     * Gets the island where stands the damaged Player
     * @return the island where stands the damaged Player
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the Player who is receiving Acid
     * @return the damaged Player
     */
    public Player getPlayer(){
        return player;
    }
    
    /**
     * Gets the amount of damage that is applied to the Player
     * @return the amount of damage caused by the acid
     */
    public double getDamage(){
        return damage;
    }
    
    /**
     * Sets the amount of damage that will be applied to the Player
     * @param damage - the amount of damage caused by the acid
     */
    public void setDamage(double damage){
        this.damage = damage;
    }
    
    /**
     * Gets the cause of the acid damage
     * @return the cause of the acid damage
     */
    public Acid getCause(){
        return cause;
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
