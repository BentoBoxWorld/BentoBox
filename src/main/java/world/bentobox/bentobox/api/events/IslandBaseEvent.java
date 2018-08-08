package world.bentobox.bentobox.api.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import world.bentobox.bentobox.database.objects.Island;

/**
 *
 * @author Poslovitch
 * @version 1.0
 */
public class IslandBaseEvent extends PremadeEvent implements Cancellable {
    private boolean cancelled;

    protected final Island island;
    protected final UUID playerUUID;
    protected final boolean admin;
    protected final Location location;

    public IslandBaseEvent(Island island) {
        super();
        this.island = island;
        playerUUID = island == null ? null : island.getOwner();
        admin = false;
        location = island == null ? null : island.getCenter();
    }

    /**
     * @param island - island
     * @param playerUUID - the player's UUID
     * @param admin - true if ths is due to an admin event
     * @param location - the location
     */
    public IslandBaseEvent(Island island, UUID playerUUID, boolean admin, Location location) {
        super();
        this.island = island;
        this.playerUUID = playerUUID;
        this.admin = admin;
        this.location = location;
    }

    /**
     * @return the island involved in this event
     */
    public Island getIsland(){
        return island;
    }

    /**
     * @return the owner of the island
     */
    public UUID getOwner() {
        return island.getOwner();
    }

    /**
     * @return the playerUUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * @return the admin
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
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
