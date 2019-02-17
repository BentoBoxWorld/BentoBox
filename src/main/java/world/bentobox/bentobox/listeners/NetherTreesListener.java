package world.bentobox.bentobox.listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;

/**
 * Handles conversion of trees in the Nether if {@link WorldSettings#isNetherTrees()} is {@code true}.
 *
 * @author tastybento
 */
public class NetherTreesListener implements Listener {

    private BentoBox plugin;

    public NetherTreesListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Converts trees to gravel and glowstone.
     *
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent e) {
        // Don't do anything if we're not in the right place.
        if (!plugin.getIWM().isNetherTrees(e.getWorld()) || !e.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            return;
        }

        // Modify everything!
        for (BlockState b : e.getBlocks()) {
            if (Tag.LOGS.isTagged(b.getType())) {
                b.setType(Material.GRAVEL);
            } else if (Tag.LEAVES.isTagged(b.getType())) {
                b.setType(Material.GLOWSTONE);
            }
        }
    }
}
