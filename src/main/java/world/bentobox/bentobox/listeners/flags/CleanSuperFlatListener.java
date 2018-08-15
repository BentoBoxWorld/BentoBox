/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 *
 */
public class CleanSuperFlatListener extends AbstractFlagListener {
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        BentoBox plugin = BentoBox.getInstance();
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
