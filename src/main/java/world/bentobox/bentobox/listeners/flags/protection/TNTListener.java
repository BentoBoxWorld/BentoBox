package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects islands from visitors blowing things up
 * @author tastybento
 */
public class TNTListener extends FlagListener {

    /**
     * Contains {@link EntityType}s that generates an explosion.
     * @since 1.5.0
     */
    private List<EntityType> tntTypes = Arrays.asList(EntityType.PRIMED_TNT, EntityType.MINECART_TNT);

    /**
     * Contains {@link Material}s that can be used to prime a TNT.
     * @since 1.5.0
     */
    private List<Material> primingItems = Arrays.asList(Material.FLINT_AND_STEEL, Material.FIRE_CHARGE);

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
                    && !checkIsland(e, (Player)projectile.getShooter(), e.getBlock().getLocation(), Flags.TNT_PRIMING)) {
                // Remove the arrow
                projectile.remove();
                e.setCancelled(true);
            }
        }
    }

    /**
     * Protect against priming of TNT unless TNT priming is allowed
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTNTPriming(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && e.getClickedBlock() != null
                && e.getClickedBlock().getType().equals(Material.TNT)
                && primingItems.contains(e.getMaterial())) {
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.TNT_PRIMING);
        }
    }

    /**
     * Prevents TNT explosion from breaking blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        // Remove any blocks from the explosion list if they are inside a protected area
        if (tntTypes.contains(e.getEntityType())
                && e.blockList().removeIf(b -> getIslands().getProtectedIslandAt(b.getLocation()).map(i -> !i.isAllowed(Flags.TNT_DAMAGE)).orElse(false))) {
            // If any were removed
            e.setCancelled(true); // Seems to have no effect.
        }
    }

    /**
     * Prevents TNT explosion from damaging entities.
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityDamageByEntityEvent e) {
        // Check if this a TNT exploding
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) || !tntTypes.contains(e.getDamager().getType())) {
            return;
        }

        // Check if it is disallowed, then cancel it.
        if(getIslands().getProtectedIslandAt(e.getEntity().getLocation()).map(i -> !i.isAllowed(Flags.TNT_DAMAGE)).orElse(false)) {
            e.setCancelled(true);
        }
    }
}
