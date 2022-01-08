package world.bentobox.bentobox.listeners.flags.protection;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
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
    private static final List<EntityType> TNT_TYPES = List.of(EntityType.PRIMED_TNT, EntityType.MINECART_TNT);

    /**
     * Contains {@link Material}s that can be used to prime a TNT.
     * @since 1.5.0
     */
    private static final List<Material> PRIMING_ITEMS = List.of(Material.FLINT_AND_STEEL, Material.FIRE_CHARGE);

    /**
     * Protect TNT from being set light by a fire arrow
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public boolean onTNTDamage(EntityChangeBlockEvent e) {
        // Check world
        if (!e.getBlock().getType().equals(Material.TNT) || !getIWM().inWorld(e.getBlock().getLocation())) {
            return false;
        }
        // Stop TNT from being damaged if it is being caused by a visitor with a flaming arrow
        if (e.getEntity() instanceof Projectile projectile) {
            // Find out who fired it
            if (projectile.getShooter() instanceof Player shooter && projectile.getFireTicks() > 0
                    && !checkIsland(e, shooter, e.getBlock().getLocation(), Flags.TNT_PRIMING)) {
                // Remove the arrow
                projectile.remove();
                e.setCancelled(true);
                return true;
            }
        }
        return false;
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
                && PRIMING_ITEMS.contains(e.getMaterial())) {
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.TNT_PRIMING);
        }
    }

    /**
     * Prevents TNT explosion from breaking blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        // Check world and types
        if (!getIWM().inWorld(e.getLocation()) || !TNT_TYPES.contains(e.getEntityType())) {
            return;
        }

        if (protect(e.getLocation())) {
            // This is protected as a whole, so just cancel the event
            e.setCancelled(true);
        } else {
            // Remove any blocks from the explosion list if required
            e.blockList().removeIf(b -> protect(b.getLocation()));
        }
    }

    protected boolean protect(Location location) {
        return getIslands().getProtectedIslandAt(location).map(i -> !i.isAllowed(Flags.TNT_DAMAGE))
                .orElseGet(() -> !Flags.WORLD_TNT_DAMAGE.isSetForWorld(location.getWorld()));
    }

    /**
     * Prevents TNT explosion from damaging entities.
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityDamageByEntityEvent e) {
        // Check if this in world, an explosion, and TNT exploding
        if (getIWM().inWorld(e.getEntity().getLocation())
                && e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                && TNT_TYPES.contains(e.getDamager().getType())) {
            // Check if it is disallowed, then cancel it.
            e.setCancelled(protect(e.getEntity().getLocation()));
        }
    }

    protected boolean protectBlockExplode(Location location) {
        return getIslands().getProtectedIslandAt(location).map(i -> !i.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE))
                .orElseGet(() -> !Flags.WORLD_BLOCK_EXPLODE_DAMAGE.isSetForWorld(location.getWorld()));
    }

    /**
     * Prevents block explosion from breaking blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final BlockExplodeEvent e) {
        // Check world
        if (!getIWM().inWorld(e.getBlock().getLocation())) {
            return;
        }

        if (protectBlockExplode(e.getBlock().getLocation())) {
            // This is protected as a whole, so just cancel the event
            e.setCancelled(true);
        } else {
            // Remove any blocks from the explosion list if required
            e.blockList().removeIf(b -> protectBlockExplode(b.getLocation()));
        }
    }

    /**
     * Prevents block explosion from damaging entities.
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityDamageEvent e) {
        // Check if this in world and a block explosion
        if (getIWM().inWorld(e.getEntity().getLocation())
                && e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            // Check if it is disallowed, then cancel it.
            e.setCancelled(protectBlockExplode(e.getEntity().getLocation()));
        }
    }
}
