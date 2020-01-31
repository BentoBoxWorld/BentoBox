package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link world.bentobox.bentobox.lists.Flags#TREES_GROWING_OUTSIDE_RANGE}.
 * @author Poslovitch
 * @since 1.3.0
 */
public class TreesGrowingOutsideRangeListener extends FlagListener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onTreeGrow(StructureGrowEvent e) {
        if (!getIWM().inWorld(e.getWorld()) || Flags.TREES_GROWING_OUTSIDE_RANGE.isSetForWorld(e.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        // If there is no protected island at the location of the sapling, just cancel the event (prevents the sapling from growing).
        if (!getIslands().getProtectedIslandAt(e.getLocation()).isPresent()) {
            e.setCancelled(true);
            return;
        }

        // Now, run through all the blocks that will be generated and if there is no protected island at their location, turn them into AIR.
        e.getBlocks().stream()
        .filter(blockState -> !getIslands().getProtectedIslandAt(blockState.getLocation()).isPresent())
        .forEach(blockState -> blockState.setType(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChorusGrow(BlockSpreadEvent e) {
        if (!getIWM().inWorld(e.getSource().getWorld()) || Flags.TREES_GROWING_OUTSIDE_RANGE.isSetForWorld(e.getSource().getWorld())
                || !e.getSource().getType().equals(Material.CHORUS_FLOWER)) {
            // We do not want to run any check if this is not the right world or if it is allowed
            // The same goes if this is not a Chorus Flower that is growing.
            return;
        }

        // If there is no protected island at the location of the chorus flower, just cancel the event (prevents the flower from growing).
        if (!getIslands().getProtectedIslandAt(e.getSource().getLocation()).isPresent()) {
            e.setCancelled(true);
            return;
        }

        // Now prevent the flower to grow if this is growing outside the island
        if (!getIslands().getProtectedIslandAt(e.getBlock().getLocation()).isPresent()) {
            e.setCancelled(true);
        }
    }
}
