package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import org.bukkit.event.Cancellable;
import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * @author tastybento
 */
public abstract class PlayerBaseEvent extends BentoBoxEvent implements Cancellable {
    private boolean cancelled;

    protected final Island island;
    protected final UUID playerUUID;
    protected final World world;

    public PlayerBaseEvent(UUID playerUUID, Island island, World world) {
        super();
        this.playerUUID = playerUUID;
        this.world = world;
        this.island = island;
    }

    /**
     * @return the island involved in this event. This may be null if the event is not related to an island
     * or if the island has been deleted.
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Get the world involved in this event.
     * @return world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the playerUUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

}
