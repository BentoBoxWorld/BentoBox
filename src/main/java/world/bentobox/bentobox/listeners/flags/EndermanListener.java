/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Listens for Endermen
 * For the {@link world.bentobox.bentobox.lists.Flags#ENDERMAN_GRIEFING}
 * and {@link world.bentobox.bentobox.lists.Flags#CREEPER_GRIEFING} flags.
 * @author tastybento
 *
 */
public class EndermanListener extends AbstractFlagListener {
    /**
     * Allows or prevents enderman griefing
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanGrief(final EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof Enderman) || !getIWM().inWorld(e.getEntity().getLocation())) {
            return;
        }
        if (!Flags.ENDERMAN_GRIEFING.isSetForWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

}
