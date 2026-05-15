package world.bentobox.bentobox.managers;

import org.bukkit.Location;

import world.bentobox.bentobox.BentoBox;

/**
 * Reports whether an island location is awaiting filesystem-level cleanup
 * (i.e. the row is still in the DB with {@code deletable = true}, before
 * {@code PurgeRegionsService} reaps the region files).
 *
 * <p>Backed by the live island state rather than a side-channel set, so
 * it can never drift out of sync with the actual database.
 *
 * @author tastybento
 * @since 1.1
 */
public class IslandDeletionManager {

    private final BentoBox plugin;

    public IslandDeletionManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if an island location is in deletion
     * @param location - center of location
     * @return true if there is an island at this location that is marked deletable
     */
    public boolean inDeletion(Location location) {
        return plugin.getIslands().getIslandAt(location).map(island -> island.isDeletable()).orElse(false);
    }

}
