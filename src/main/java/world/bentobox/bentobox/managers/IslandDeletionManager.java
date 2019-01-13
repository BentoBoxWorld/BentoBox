package world.bentobox.bentobox.managers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeleteChunksEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeletedEvent;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.DeletedIslandDO;
import world.bentobox.bentobox.util.DeleteIslandChunks;
import world.bentobox.bentobox.util.Util;

/**
 * Listens for island deletions and adds them to the database. Removes them when the island is deleted.
 * @author tastybento
 * @since 1.1
 */
public class IslandDeletionManager implements Listener {

    private BentoBox plugin;
    /**
     * Queue of islands to delete
     */
    private Database<DeletedIslandDO> handler;
    private Set<Location> inDeletion;

    public IslandDeletionManager(BentoBox plugin) {
        this.plugin = plugin;
        handler = new Database<>(plugin, DeletedIslandDO.class);
        inDeletion = new HashSet<>();
    }

    /**
     * When BentoBox is fully loaded, load the islands that still need to be deleted and kick them off
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Load list of islands that were mid deletion and delete them
        List<DeletedIslandDO> toBeDeleted = handler.loadObjects();
        if (toBeDeleted != null && toBeDeleted.size() > 0) {
            plugin.log("There are " + toBeDeleted.size() + " islands pending deletion.");
            toBeDeleted.forEach(di -> {
                plugin.log("Resuming deletion of island at " + di.getLocation().getWorld().getName() + " " + Util.xyz(di.getLocation().toVector()));
                inDeletion.add(di.getLocation());
                new DeleteIslandChunks(plugin, di);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteChunksEvent e) {
        // Store location
        inDeletion.add(e.getDeletedIslandInfo().getLocation());
        // Save to database
        handler.saveObject(e.getDeletedIslandInfo());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDeleted(IslandDeletedEvent e) {
        // Delete
        inDeletion.remove(e.getDeletedIslandInfo().getLocation());
        // Delete from database
        handler.deleteID(e.getDeletedIslandInfo().getUniqueId());
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
