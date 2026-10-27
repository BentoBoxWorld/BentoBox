package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents pistons from pushing or pulling blocks outside island protection range
 * @author tastybento
 *
 */
public class PistonPushListener extends FlagListener {

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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (Flags.PISTON_PUSH.isSetForWorld(e.getBlock().getWorld())) {
            getIslands().getProtectedIslandAt(e.getBlock().getLocation()).ifPresent(i ->
            // A pulled block stays where it is, so its own location is what has to be on the island
            e.setCancelled(!e.getBlocks().stream().map(org.bukkit.block.Block::getLocation).allMatch(i::onIsland)));
        }
    }
}
