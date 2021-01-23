package world.bentobox.bentobox.api.events;

import org.bukkit.event.HandlerList;

/**
 * Fired when plugin is ready to play and all files are loaded
 *
 * @author tastybento
 */
public class BentoBoxReadyEvent extends BentoBoxEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
