package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player joins an existing island.
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandJoinEvent extends IslandEvent {
    private final UUID player;

    /**
     * @param island
     * @param player
     */
    public IslandJoinEvent(Island island, UUID player) {
        super(island);
        this.player = player;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Convenience method suggested by Exloki.
     * Equals to <code>getIsland().getOwner();</code>
     * @return the owner of the joined island
     */
    public UUID getNewIslandOwner() {
        return getIsland().getOwner();
    }
}
