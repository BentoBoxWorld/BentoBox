package us.tastybento.bskyblock.api.events.acid;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when an entity (player and items excluded) receives damage from acid
 * @author Poslovitch
 * @since 4.0
 */
public class EntityDamageByAcidEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    
    private Island island;
    private Entity entity;
    private double damage;
    
    public EntityDamageByAcidEvent(Island island, Entity entity, double damage) {
        this.island = island;
        this.entity = entity;
        this.damage = damage;
    }
    
    /**
     * Gets the island where stands the damaged Entity
     * @return the island where stands the damaged Entity
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * Gets the Entity who is receiving Acid
     * @return the damaged Entity
     */
    public Entity getEntity(){
        return entity;
    }
    
    /**
     * Gets the amount of damage that is applied to the Entity
     * @return the amount of damage caused by the acid
     */
    public double getDamage(){
        return damage;
    }
    
    /**
     * Sets the amount of damage that will be applied to the entity
     * @param damage - the amount of damage caused by the acid
     */
    public void setDamage(double damage){
        this.damage = damage;
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
