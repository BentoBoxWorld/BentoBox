/**
 *
 */
package world.bentobox.bbox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import world.bentobox.bbox.api.flags.AbstractFlagListener;
import world.bentobox.bbox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class ChestDamageListener extends AbstractFlagListener {
    /**
     * Prevent chest damage from explosion
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (getIWM().inWorld(e.getLocation()) && !Flags.CHEST_DAMAGE.isSetForWorld(e.getLocation().getWorld())) {
            e.blockList().removeIf(b -> b.getType().equals(Material.CHEST) || b.getType().equals(Material.TRAPPED_CHEST));
        }
    }
}
