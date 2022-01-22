package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when a player will be banned from an island.
 * May be cancelled.
 * Cancellation will result in the ban being aborted.
 *
 * @since 1.1
 */
public class IslandBanEvent extends IslandBaseEvent {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandBanEvent(Island island, UUID player, boolean admin, Location location) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
    }
}