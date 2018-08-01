/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

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

    /**
     * Drops the Enderman's block when he dies if he has one
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanDeath(final EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Enderman)
                || !getIWM().inWorld(e.getEntity().getLocation())
                || !Flags.ENDERMAN_DEATH_DROP.isSetForWorld(e.getEntity().getWorld())) {
            return;
        }
        // Get the block the enderman is holding
        Enderman ender = (Enderman) e.getEntity();
        BlockData m = ender.getCarriedBlock();
        if (m != null && !m.getMaterial().equals(Material.AIR)) {
            // Drop the item
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(m.getMaterial()));
        }
    }

}
