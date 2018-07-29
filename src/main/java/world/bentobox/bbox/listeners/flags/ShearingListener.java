package world.bentobox.bbox.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;

import world.bentobox.bbox.api.flags.AbstractFlagListener;
import world.bentobox.bbox.lists.Flags;

/**
 * Handles shearing
 * @author tastybento
 *
 */
public class ShearingListener extends AbstractFlagListener {

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
        checkIsland(e, e.getEntity().getLocation(), Flags.SHEARING);
    }

}
