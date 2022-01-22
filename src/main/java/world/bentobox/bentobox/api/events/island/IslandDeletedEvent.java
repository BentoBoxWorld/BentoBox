package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;

/**
 * Fired when island blocks are going to be deleted.
 * If canceled, the island blocks will not be deleted. Note that by the time this is called
 * the ownership of the island may have been removed. This event is just for detecting
 * that the island blocks are going to be removed.
 *
 */
public class IslandDeletedEvent extends IslandBaseEvent {
    private final IslandDeletion deletedIslandInfo;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandDeletedEvent(Island island, UUID player, boolean admin, Location location, IslandDeletion deletedIsland) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
        this.deletedIslandInfo = deletedIsland;
    }

    public IslandDeletion getDeletedIslandInfo() {
        return deletedIslandInfo;
    }
}