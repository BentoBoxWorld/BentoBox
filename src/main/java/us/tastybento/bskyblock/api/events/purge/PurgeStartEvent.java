package us.tastybento.bskyblock.api.events.purge;

import java.util.List;
import java.util.UUID;

import org.bukkit.event.Cancellable;

import us.tastybento.bskyblock.api.events.PremadeEvent;

/**
 * This event is fired when islands to remove have been chosen and before starting to remove them.
 * You can remove or add islands to remove.
 * Canceling this event will cancel the purge
 *
 * @author Poslovitch
 * @since 1.0
 */
public class PurgeStartEvent extends PremadeEvent implements Cancellable {
    private boolean cancelled;

    private final UUID user;
    private List<UUID> islandsList;

    /**
     * Called to create the event
     * @param user - the User - the UUID of the player who launched the purge, may be null if purge is launched using the console.
     * @param islandsList - the list of islands to remove, based on their leader's UUID
     */
    public PurgeStartEvent(UUID user, List<UUID> islandsList) {
        this.user = user;
        this.islandsList = islandsList;
    }

    /**
     * @return the user who launched the purge, may be null if purge is launched using the console.
     */
    public UUID getUser( ){
        return user;
    }

    /**
     * @return the list of islands to remove, based on their leader's UUID
     */
    public List<UUID> getIslandsList() {
        return islandsList;
    }

    /**
     * Convenience method to directly add an island owner's UUID to the list
     * @param islandOwner - the owner's UUID from the island to remove
     */
    public void add(UUID islandOwner) {
        if(!islandsList.contains(islandOwner)) {
            islandsList.add(islandOwner);
        }
    }

    /**
     * Convenience method to directly remove an island owner's UUID to the list
     * @param islandOwner - the owner's UUID from the island to remove
     */
    public void remove(UUID islandOwner) {
        if(islandsList.contains(islandOwner)) {
            islandsList.remove(islandOwner);
        }
    }

    /**
     * Replace the island list
     * @param islandsList - a new island owners' UUIDs list
     */
    public void setIslandsList(List<UUID> islandsList) {
        this.islandsList = islandsList;
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
