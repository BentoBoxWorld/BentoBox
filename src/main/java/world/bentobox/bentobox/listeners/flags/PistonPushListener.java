/*

 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents pistons from pushing blocks outside island protection range
 * @author tastybento
 *
 */
public class PistonPushListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        // Only process if flag is active
        if (Flags.PISTON_PUSH.isSetForWorld(e.getBlock().getWorld())) {
            getIslands().getProtectedIslandAt(e.getBlock().getLocation()).ifPresent(i -> 
            e.setCancelled(
                    // Run through the location of all the relative blocks and see if they are outside the island
                    !e.getBlocks().stream()
                    .map(b -> b.getRelative(e.getDirection()).getLocation())
                    // All blocks must be on the island, otherwise the event is cancelled
                    .allMatch(i::onIsland)));
        }
    }
}
