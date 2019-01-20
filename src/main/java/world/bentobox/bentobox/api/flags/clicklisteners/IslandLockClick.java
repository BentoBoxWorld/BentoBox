/**
 *
 */
package world.bentobox.bentobox.api.flags.clicklisteners;

import world.bentobox.bentobox.api.events.island.IslandEvent;

/**
 * Handles clicking on the lock icon
 * @author tastybento
 *
 */
public class IslandLockClick extends CycleClick {

    /**
     * @param id
     */
    public IslandLockClick(String id) {
        super(id);
    }

    /**
     * @param id
     * @param minRank
     * @param maxRank
     */
    public IslandLockClick(String id, int minRank, int maxRank) {
        super(id, minRank, maxRank);
        if (island != null && changeOccurred) {
            // Fire lock event
            new IslandEvent.IslandEventBuilder()
            .island(island)
            .involvedPlayer(user.getUniqueId())
            .reason(IslandEvent.Reason.LOCK)
            .admin(false)
            .location(user.getLocation())
            .build();
        }
    }

}
