package us.tastybento.bskyblock.api.events.island;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired when an island is going to be unlocked.
 * <p>
 * Cancelling this event will result in keeping the island locked.
 * 
 * @author Poslovitch
 * @since 1.0
 */
public class IslandUnlockEvent extends IslandEvent {
    private final CommandSender unlocker;

    /**
     * @param island
     * @param unlocker
     */
    public IslandUnlockEvent(Island island, CommandSender unlocker) {
        super(island);
        this.unlocker = unlocker;
    }

    public CommandSender getUnlocker(){
        return unlocker;
    }
}