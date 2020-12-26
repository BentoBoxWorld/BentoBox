package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an island is going to be deleted.
 * May be cancelled.
 *
 */
public class IslandDeleteEvent extends IslandBaseEvent {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandDeleteEvent(Island island, UUID player, boolean admin, Location location) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
    }
}