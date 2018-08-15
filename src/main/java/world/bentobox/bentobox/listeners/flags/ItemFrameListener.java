/**
 *
 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects item frames from damage by mobs
 * @author tastybento
 *
 */
public class ItemFrameListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemFrameDamage(final EntityDamageByEntityEvent e) {
        check(e, e.getEntity(), e.getDamager());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemFrameDamage(final HangingBreakByEntityEvent e) {
        check(e, e.getEntity(), e.getRemover());
    }

    private void check(Cancellable e, Entity entity, Entity damager) {
        if (entity instanceof ItemFrame
                && getIWM().inWorld(entity.getLocation())
                && !Flags.ITEM_FRAME_DAMAGE.isSetForWorld(entity.getWorld())
                && !(damager instanceof Player)) {
            if (damager instanceof Projectile) {
                if (!(((Projectile) damager).getShooter() instanceof Player)) {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

}
