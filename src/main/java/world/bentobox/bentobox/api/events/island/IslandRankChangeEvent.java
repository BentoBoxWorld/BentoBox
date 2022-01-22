package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when a player's rank has changed on an island.
 * Cancellation has no effect.
 * @since 1.13.0
 */
public class IslandRankChangeEvent extends IslandBaseEvent {

    private final int oldRank;
    private final int newRank;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public IslandRankChangeEvent(Island island, UUID playerUUID, boolean admin, Location location, int oldRank, int newRank) {
        super(island, playerUUID, admin, location);
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public int getOldRank() {
        return oldRank;
    }

    public int getNewRank(){
        return newRank;
    }
}