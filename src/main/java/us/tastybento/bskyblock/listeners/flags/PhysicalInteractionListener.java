/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import us.tastybento.bskyblock.lists.Flag;

/**
 * @author ben
 *
 */
public class PhysicalInteractionListener extends AbstractFlagListener {

    /**
     * Handle physical interaction with blocks
     * Crop trample, pressure plates, triggering redstone, tripwires
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL)) {
            return;
        }
        switch (e.getClickedBlock().getType()) {
        case SOIL:
            // Crop trample
            checkIsland(e, e.getPlayer().getLocation(), Flag.CROP_TRAMPLE);
            break;
        case WOOD_PLATE:
        case STONE_PLATE:
        case GOLD_PLATE:
        case IRON_PLATE:
            // Pressure plates
            checkIsland(e, e.getPlayer().getLocation(), Flag.PRESSURE_PLATE);
            break;
        default:
            break;

        }
    }
}
