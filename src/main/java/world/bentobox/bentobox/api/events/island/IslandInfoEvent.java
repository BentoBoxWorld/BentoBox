package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an a player reuqets info about an island
 * Cancellation has no effect.
 */
public class IslandInfoEvent extends IslandBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @param island island
     * @param player player asking for the info
     * @param admin true if this is an admin request
     * @param location location of the player asking for the info
     */
    public IslandInfoEvent(Island island, UUID player, boolean admin, Location location) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
    }

}