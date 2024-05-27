package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles shearing
 * @author tastybento
 *
 */
public class ShearingListener extends FlagListener {

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
        checkIsland(e, e.getPlayer(), e.getEntity().getLocation(), Flags.SHEARING);
    }

    // Block shearing - paper only
    @EventHandler(priority = EventPriority.LOW)
    public void onShearBlock(final PlayerShearBlockEvent e) {
        checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.SHEARING);
    }

}
