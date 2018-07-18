/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.AbstractFlagListener;
import us.tastybento.bskyblock.lists.Flags;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 *
 */
public class CleanSuperFlatListener extends AbstractFlagListener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        BSkyBlock plugin = BSkyBlock.getInstance();
        World world = e.getWorld();
        if (!e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK)
                || !Flags.CLEAN_SUPER_FLAT.isSetForWorld(world)
                || (world.getEnvironment().equals(Environment.NETHER) && (!plugin.getIWM().isNetherGenerate(world) || !plugin.getIWM().isNetherIslands(world)))
                || (world.getEnvironment().equals(Environment.THE_END) && (!plugin.getIWM().isEndGenerate(world) || !plugin.getIWM().isEndIslands(world)))) {
            return;
        }
        world.regenerateChunk(e.getChunk().getX(), e.getChunk().getZ());
        plugin.logWarning("Regenerating superflat chunk in " + world.getName() + " at blocks " + (e.getChunk().getX() * 16) + "," + (e.getChunk().getZ() * 16));

    }

}
