package world.bentobox.bentobox.api.events;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.objects.Island;

/**
 * @author Poslovitch, tastybento
 */
public abstract class IslandBaseEvent extends BentoBoxEvent implements Cancellable {
    private boolean cancelled;

    protected final Island island;
    protected final UUID playerUUID;
    protected final boolean admin;
    protected final Location location;
    protected final Event rawEvent;
    protected IslandBaseEvent newEvent;

    public IslandBaseEvent(Island island) {
        super();
        this.island = island;
        playerUUID = island == null ? null : island.getOwner();
        admin = false;
        location = island == null ? null : island.getCenter();
        rawEvent = null;
    }

    /**
     * @param island     - island
     * @param playerUUID - the player's UUID
     * @param admin      - true if ths is due to an admin event
     * @param location   - the location
     */
    public IslandBaseEvent(Island island, UUID playerUUID, boolean admin, Location location) {
        super();
        this.island = island;
        this.playerUUID = playerUUID;
        this.admin = admin;
        if (location != null) {
            this.location = location;
        } else if (island != null) {
            this.location = island.getCenter();
        } else {
            this.location = null;
        }
        rawEvent = null;
    }

    /**
     * @param island     - island
     * @param playerUUID - the player's UUID
     * @param admin      - true if ths is due to an admin event
     * @param location   - the location
     * @param rawEvent   - the raw event
     */
    public IslandBaseEvent(Island island, UUID playerUUID, boolean admin, Location location, Event rawEvent) {
        super();
        this.island = island;
        this.playerUUID = playerUUID;
        this.admin = admin;
        if (location != null) {
            this.location = location;
        } else if (island != null) {
            this.location = island.getCenter();
        } else {
            this.location = null;
        }
        this.rawEvent = rawEvent;
    }

    /**
     * @return the island involved in this event. This may be null in the case of
     *         deleted islands, so use location instead
     */
    public Island getIsland() {
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
    @Nullable
    public Location getLocation() {
        return location;
    }

    /**
     * @return the raw event
     */
    @Nullable
    public Event getRawEvent() {
        return rawEvent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    /**
     * Get new event if this event is deprecated
     * 
     * @return optional newEvent or empty if there is none
     */
    public Optional<IslandBaseEvent> getNewEvent() {
        return Optional.ofNullable(newEvent);
    }

    /**
     * Set the newer event so it can be obtained if this event is deprecated
     * 
     * @param newEvent the newEvent to set
     */
    public void setNewEvent(IslandBaseEvent newEvent) {
        this.newEvent = newEvent;
    }
}
