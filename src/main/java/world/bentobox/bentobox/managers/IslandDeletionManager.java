package world.bentobox.bentobox.managers;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandDeletedEvent;

/**
 * Tracks island locations that are currently being deleted so callers
 * (e.g. island-creation placement, register command) can avoid them.
 *
 * @author tastybento
 * @since 1.1
 */
public class IslandDeletionManager implements Listener {

    private final Set<Location> inDeletion;

    public IslandDeletionManager(BentoBox plugin) {
        inDeletion = new HashSet<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDeleted(IslandDeletedEvent e) {
        inDeletion.remove(e.getDeletedIslandInfo().getLocation());
    }

    /**
     * Check if an island location is in deletion
     * @param location - center of location
     * @return true if island is in the process of being deleted
     */
    public boolean inDeletion(Location location) {
        return inDeletion.contains(location);
    }

}
