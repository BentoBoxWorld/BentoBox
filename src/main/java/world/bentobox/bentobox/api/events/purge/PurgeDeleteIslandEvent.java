package world.bentobox.bentobox.api.events.purge;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * This event is fired before an island is going to be purged.
 * Canceling this event will prevent the plugin to remove the island.
 *
 * @author Poslovitch
 * @since 1.0
 */
public class PurgeDeleteIslandEvent extends IslandBaseEvent {

    /**
     * Called to create the event
     * @param island - island that will be removed
     */
    public PurgeDeleteIslandEvent(Island island) {
        super(island);
    }
}
