package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Listens for creepers hsssssssh!
 * For the {@link world.bentobox.bentobox.lists.Flags#CREEPER_DAMAGE}
 * and {@link world.bentobox.bentobox.lists.Flags#CREEPER_GRIEFING} flags.
 * @author tastybento
 *
 */
public class CreeperListener extends FlagListener {

    /**
     * Prevent blocks being destroyed from explosion
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (!e.getEntityType().equals(EntityType.CREEPER) || !getIWM().inWorld(e.getLocation())) {
            return;
        }
        // If creeper damage is not allowed in world, remove it
        if (!Flags.CREEPER_DAMAGE.isSetForWorld(e.getLocation().getWorld())) {
            // If any were removed, then prevent damage too
            e.blockList().clear();
            e.setCancelled(true);
            return;
        }
        // Check for griefing
        Creeper creeper = (Creeper)e.getEntity();
        if (!Flags.CREEPER_GRIEFING.isSetForWorld(e.getLocation().getWorld()) && creeper.getTarget() instanceof Player) {
            Player target = (Player)creeper.getTarget();
            if (!getIslands().locationIsOnIsland(target, e.getLocation())) {
                User user = User.getInstance(target);
                user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(Flags.CREEPER_GRIEFING.getHintReference()));
                e.setCancelled(true);
                e.blockList().clear();
            }
        }
    }

    /**
     * Prevent entities being damaged by explosion
     * @param e - event
     * @since 1.10.0
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityDamageByEntityEvent e) {
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) || !getIWM().inWorld(e.getEntity().getLocation())
                || !e.getDamager().getType().equals(EntityType.CREEPER)) {
            return;
        }
        // If creeper damage is not allowed in world cancel the damage
        if (!Flags.CREEPER_DAMAGE.isSetForWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }
}
