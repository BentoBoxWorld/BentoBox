package us.tastybento.bskyblock.util;

import org.bukkit.World;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.database.objects.Island;

//import com.wasteofplastic.askyblock.nms.NMSAbstraction;

/**
 * Deletes islands fast using chunk regeneration
 *
 * @author tastybento
 *
 */
public class DeleteIslandChunks {

    /**
     * Deletes the island
     * @param plugin
     * @param island
     */
    public DeleteIslandChunks(final BSkyBlock plugin, final Island island) {
        //plugin.getLogger().info("DEBUG: deleting the island");
        // Fire event
        IslandBaseEvent event = IslandEvent.builder().island(island).reason(Reason.DELETE).build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        final World world = island.getCenter().getWorld();
        if (world == null)
            return;
        int minXChunk = island.getMinX() / 16;
        int maxXChunk = (island.getRange() * 2 + island.getMinX() - 1) /16;
        int minZChunk = island.getMinZ() / 16;
        int maxZChunk = (island.getRange() * 2 + island.getMinZ() - 1) /16;
        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z<=maxZChunk; z++) {
                world.regenerateChunk(x, z);
                if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands()) {
                    plugin.getIslandWorldManager().getNetherWorld().regenerateChunk(x, z);

                }
                if (plugin.getSettings().isEndGenerate() && plugin.getSettings().isEndIslands()) {
                    plugin.getIslandWorldManager().getEndWorld().regenerateChunk(x, z);
                }
            }
        }
        // Fire event
        IslandBaseEvent event1 = IslandEvent.builder().island(island).reason(Reason.DELETED).build();
        plugin.getServer().getPluginManager().callEvent(event1);

    }

}