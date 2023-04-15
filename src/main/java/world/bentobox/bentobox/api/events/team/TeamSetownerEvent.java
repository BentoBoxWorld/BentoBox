package world.bentobox.bentobox.api.events.team;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Event fires before a setowner is performed on an island.
 * To get the old owner, get from the island object. The new owner is the player's UUID.
 * @author tastybento
 *
 */
public class TeamSetownerEvent extends IslandBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public TeamSetownerEvent(Island island, UUID player, boolean admin, Location location) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
    }
    /**
     * Convenience method to get the old owner of the island
     * @return UUID of old owner
     */
    public UUID getOldOwner() {
        return island.getOwner();
    }
    /**
     * Convenience method to get the new owner of the island
     * @return UUID of new owner
     */
    public UUID getNewOwner() {
        return playerUUID;
    }
}