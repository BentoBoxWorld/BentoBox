package world.bentobox.bentobox.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.DeletedIslandDO;

/**
 * Deletes islands fast using chunk regeneration
 *
 * @author tastybento
 *
 */
public class DeleteIslandChunks {

    private int x;
    private int z;
    private BukkitTask task;

    @SuppressWarnings("deprecation")
    public DeleteIslandChunks(BentoBox plugin, DeletedIslandDO di) {
        x = di.getMinXChunk();
        z = di.getMinZChunk();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            di.getWorld().regenerateChunk(x, z);
            //System.out.println("regenerating = " + x + "," + z);
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
                    task.cancel();
                    // Fire event
                    IslandEvent.builder().location(Util.getLocationString(di.getUniqueId())).reason(Reason.DELETED).build();
                }
            }
        }, 0L, 1L);

    }}