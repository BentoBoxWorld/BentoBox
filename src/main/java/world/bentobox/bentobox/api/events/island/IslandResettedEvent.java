package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired after an island is reset
 *
 */
public class IslandResettedEvent extends IslandBaseEvent {

    private final @NonNull Island oldIsland;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandResettedEvent(Island island, UUID player, boolean admin, Location location, Island oldIsland) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
        // Create a copy of the old island
        this.oldIsland = new Island(oldIsland);
    }

    /**
     * @since 1.12.0
     */
    @NonNull
    public Island getOldIsland() {
        return oldIsland;
    }
}