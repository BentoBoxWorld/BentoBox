package world.bentobox.bentobox.api.flags.clicklisteners;

import world.bentobox.bentobox.api.events.island.IslandEvent;

/**
 * Handles clicking on the lock icon
 * @author tastybento
 *
 */
public class IslandLockClick extends CycleClick {

    /**
     * @param id flag id
     */
    public IslandLockClick(String id) {
        super(id);
    }

    /**
     * @param id flag id
     * @param minRank minimum rank
     * @param maxRank maximum rank
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
