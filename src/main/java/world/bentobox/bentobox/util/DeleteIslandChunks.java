package world.bentobox.bentobox.util;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Deletes islands fast using chunk regeneration
 *
 * @author tastybento
 *
 */
public class DeleteIslandChunks {

    /**
     * Deletes the island
     * @param plugin - plugin object
     * @param island - island to delete
     */
    @SuppressWarnings("deprecation")
    public DeleteIslandChunks(final BentoBox plugin, final Island island) {
        // Fire event
        IslandBaseEvent event = IslandEvent.builder().island(island).reason(Reason.DELETE).build();
        if (event.isCancelled()) {
            return;
        }
        final World world = island.getCenter().getWorld();
        if (world == null) {
            return;
        }
        int minXChunk =  island.getMinX() / 16;
        int maxXChunk = (island.getRange() * 2 + island.getMinX() - 1) /16;
        int minZChunk = island.getMinZ() / 16;
        int maxZChunk = (island.getRange() * 2 + island.getMinZ() - 1) /16;
        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z<=maxZChunk; z++) {
                world.regenerateChunk(x, z);
                if (plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world)) {
                    plugin.getIWM().getNetherWorld(world).regenerateChunk(x, z);

                }
                if (plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world)) {
                    plugin.getIWM().getEndWorld(world).regenerateChunk(x, z);
                }
            }
        }
        // Fire event
        IslandEvent.builder().island(island).reason(Reason.DELETED).build();

    }

}