/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Pair;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 *
 */
public class CleanSuperFlatListener extends FlagListener {
    
    private Set<Pair<Integer, Integer>> regeneratedChunk = new HashSet<>();
    
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        BentoBox plugin = BentoBox.getInstance();
        World world = e.getWorld();
        if (regeneratedChunk.contains(new Pair<Integer, Integer>(e.getChunk().getX(), e.getChunk().getZ()))) {
            Flags.CLEAN_SUPER_FLAT.setSetting(world, false);
            plugin.logError("World generator for " + world.getName() + " is broken and superflat regen cannot occur!!! Disabling regen.");
            return;
        }
        if (!e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK)
                || !Flags.CLEAN_SUPER_FLAT.isSetForWorld(world)
                || (world.getEnvironment().equals(Environment.NETHER) && (!plugin.getIWM().isNetherGenerate(world) || !plugin.getIWM().isNetherIslands(world)))
                || (world.getEnvironment().equals(Environment.THE_END) && (!plugin.getIWM().isEndGenerate(world) || !plugin.getIWM().isEndIslands(world)))) {
            return;
        }
        // This deprecation is OK because all it means is that things like tree leaves may not be the same in the chunk when it is generated
        world.regenerateChunk(e.getChunk().getX(), e.getChunk().getZ());
        plugin.logWarning("Regenerating superflat chunk in " + world.getName() + " at blocks " + (e.getChunk().getX() << 4) + "," + (e.getChunk().getZ() << 4));

    }

}
