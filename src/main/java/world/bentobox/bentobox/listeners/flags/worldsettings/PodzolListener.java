package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.StructureGrowEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Podzol listener - prevents generation of podzol under large trees
 * @since 3.2.4
 */
public class PodzolListener extends FlagListener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTreeGrow(StructureGrowEvent e) {
        if (!getIWM().inWorld(e.getWorld()) || Flags.PODZOL.isSetForWorld(e.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        // If there is no protected island at the location of the sapling, just cancel the event (prevents the sapling from growing).
        Optional<Island> optionalProtectedIsland = getIslands().getProtectedIslandAt(e.getLocation());
        if (optionalProtectedIsland.isEmpty()) {
            e.setCancelled(true);
            return;
        }
        // Now, run through all the blocks that will be generated and if there is no protected island at their location, or the protected island is not the same as the one growing the tree then turn them into AIR.
        e.getBlocks().removeIf(blockState -> blockState.getType() == Material.PODZOL);
    }
}
