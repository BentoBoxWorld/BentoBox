package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an a player enters an island.
 * Cancellation has no effect.
 */
public class IslandEnterEvent extends IslandBaseEvent {

    private final @Nullable Island fromIsland;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandEnterEvent(Island island, UUID player, boolean admin, Location location, @Nullable Island fromIsland, Event rawEvent) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location, rawEvent);
        this.fromIsland = fromIsland;
    }

    @Nullable
    public Island getFromIsland() {
        return fromIsland;
    }
}