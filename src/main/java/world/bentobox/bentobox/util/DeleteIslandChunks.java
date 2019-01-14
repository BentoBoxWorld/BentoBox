package world.bentobox.bentobox.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.IslandDeletion;

/**
 * Deletes islands fast using chunk regeneration
 *
 * @author tastybento
 */
public class DeleteIslandChunks {

    /**
     * This is how many chunks per world will be done in one tick.
     */
    private static final int SPEED = 5;
    private int x;
    private int z;
    private BukkitTask task;

    @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        x = di.getMinXChunk();
        z = di.getMinZChunk();
        // Run through all chunks of the islands and regenerate them.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (int i = 0; i < SPEED; i++) {
                // World#regenerateChunk(int, int) from Bukkit is deprecated because it may not regenerate decoration correctly
                di.getWorld().regenerateChunk(x, z); 
                if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
                    plugin.getIWM().getNetherWorld(di.getWorld()).regenerateChunk(x, z);
                }
                if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
                    plugin.getIWM().getEndWorld(di.getWorld()).regenerateChunk(x, z);
                }
                z++;
                if (z > di.getMaxZChunk()) {
                    z = di.getMinZChunk();
                    x++;
                    if (x > di.getMaxXChunk()) {
                        // We're done
                        task.cancel();
                        // Fire event
                        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
                    }
                }
            }
        }, 0L, 1L);
    }
}
