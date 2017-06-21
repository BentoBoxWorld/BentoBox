package us.tastybento.bskyblock.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when BSkyBlock is ready to play and all files are loaded
 * 
 * @author tastybento
 * @since 1.0
 */
public class BSkyBlockReadyEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
