package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;

/**
 * Fired when attempting to make a new island.
 * May be cancelled. No island object exists at this point.
 * @since 1.15.1
 */
public class IslandPreCreateEvent extends IslandBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandPreCreateEvent(UUID player) {
        // Final variables have to be declared in the constructor
        super(null, player, false, null);
    }
}