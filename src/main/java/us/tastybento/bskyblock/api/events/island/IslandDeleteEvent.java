package us.tastybento.bskyblock.api.events.island;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired before an island is deleted.
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandDeleteEvent extends IslandEvent {

    /**
     * @param island
     */
    public IslandDeleteEvent(Island island) {
        super(island);
    }
}
