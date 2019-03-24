package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects islands from visitors blowing things up
 * @author tastybento
 *
 */
public class TNTListener extends FlagListener {

    /**
     * Protect TNT from being set light by a fire arrow
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTNTDamage(EntityChangeBlockEvent e) {
        // Check world
        if (!e.getBlock().getType().equals(Material.TNT) || !getIWM().inWorld(e.getBlock().getLocation())) {
            return;
        }
        // Stop TNT from being damaged if it is being caused by a visitor with a flaming arrow
        if (e.getEntity() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getEntity();
            // Find out who fired it
            if (projectile.getShooter() instanceof Player && projectile.getFireTicks() > 0
                    && !checkIsland(e, (Player)projectile.getShooter(), e.getBlock().getLocation(), Flags.BREAK_BLOCKS)) {
                // Remove the arrow
                projectile.remove();
                e.setCancelled(true);
            }
        }
    }


    /**
     * Protect against priming of TNT unless break blocks is allowed
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTNTPriming(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && e.getClickedBlock() != null
                && e.getClickedBlock().getType().equals(Material.TNT)
                && e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Prevent TNT damage from explosion
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        // Remove any blocks from the explosion list if they are inside a protected area and if the entity was a TNT
        if (e.getEntityType().equals(EntityType.PRIMED_TNT)
                && e.blockList().removeIf(b -> getIslands().getProtectedIslandAt(b.getLocation()).map(i -> !i.isAllowed(Flags.TNT)).orElse(false))) {
            // If any were removed, then prevent damage too
            e.setCancelled(true);
        }
    }
}
