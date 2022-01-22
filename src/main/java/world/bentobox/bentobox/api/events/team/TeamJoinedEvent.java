package world.bentobox.bentobox.api.events.team;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Called after a player has joined an island
 * @since 1.3.0
 */
public class TeamJoinedEvent extends IslandBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Called after a player has joined an island
     * @param island - island
     * @param player - player
     * @param admin - whether this was due to an admin action
     * @param location - location
     * @since 1.3.0
     */
    TeamJoinedEvent(Island island, UUID player, boolean admin, Location location) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
    }
}