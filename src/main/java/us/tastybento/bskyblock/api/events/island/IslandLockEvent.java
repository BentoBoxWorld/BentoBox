package us.tastybento.bskyblock.api.events.island;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired when an island is going to be locked.
 * <p>
 * Cancelling this event will result in keeping the island unlocked.
 * 
 * @author Poslovitch
 * @since 1.0
 */
public class IslandLockEvent extends IslandEvent {
    private final CommandSender locker;

    /**
     * @param island
     * @param locker
     */
    public IslandLockEvent(Island island, CommandSender locker) {
        super(island);
        this.locker = locker;
    }

    public CommandSender getLocker() {
        return locker;
    }
}