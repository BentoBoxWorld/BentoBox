package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class ChestDamageListener extends FlagListener {
    /**
     * Prevent chest damage from explosion
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (getIWM().inWorld(e.getLocation()) && !Flags.CHEST_DAMAGE.isSetForWorld(e.getLocation().getWorld())) {
            e.blockList().removeIf(b -> b.getType().equals(Material.CHEST)
                    || b.getType().equals(Material.TRAPPED_CHEST)
                    || b.getType().name().contains("SHULKER_BOX")
                    );
        }
    }
}
