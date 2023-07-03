package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an a player names or renames an island.
 * Cancellation has no effect.
 */
public class IslandNameEvent extends IslandBaseEvent {

    private final String previousName;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public IslandNameEvent(Island island, UUID player, boolean admin, Location location, @Nullable String previousName) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
        this.previousName = previousName;
    }

    /**
     * @return the previous name of the island, if any. May be null if no name previously used.
     */
    @Nullable
    public String getPreviousNameIsland() {
        return previousName;
    }
}