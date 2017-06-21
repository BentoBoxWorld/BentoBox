package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when the owner of an island changes
 * 
 * @author Poslovitch
 * @version 1.0
 */
public class IslandChangeOwnerEvent extends IslandEvent {
    private final UUID oldOwner, newOwner;

    /**
     * @param island
     * @param oldOwner
     * @param newOwner
     */
    public IslandChangeOwnerEvent(Island island, UUID oldOwner, UUID newOwner) {
        super(island);
        this.oldOwner = oldOwner;
        this.newOwner = newOwner;
    }

    /**
     * @return the old owner
     */
    public UUID getOldOwner() {
        return oldOwner;
    }

    /**
     * @return the new owner
     */
    public UUID getNewOwner() {
        return newOwner;
    }
}
