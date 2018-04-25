package us.tastybento.bskyblock.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class PremadeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
