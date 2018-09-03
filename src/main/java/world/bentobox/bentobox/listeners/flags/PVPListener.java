package world.bentobox.bentobox.listeners.flags;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles PVP
 * @author tastybento
 *
 */
public class PVPListener extends AbstractFlagListener {

    private HashMap<Integer, UUID> thrownPotions = new HashMap<>();

    /**
     * This method protects players from PVP if it is not allowed and from
     * arrows fired by other players
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && getPlugin().getIWM().inWorld(e.getEntity().getLocation())) {
            // Allow self damage
            if (e.getEntity().equals(e.getDamager())) {
                return;
            }
            // Protect visitors
            if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && protectedVisitor((Player)e.getEntity())) {
                if (e.getDamager() instanceof Player) {
                    User.getInstance(e.getDamager()).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                } else if (e.getDamager() instanceof Projectile && ((Projectile)e.getDamager()).getShooter() instanceof Player) {
                    User.getInstance((Player)((Projectile)e.getDamager()).getShooter()).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                }
                e.setCancelled(true);
            } else {
                // PVP check
                respond(e, e.getDamager(), e.getEntity(), getFlag(e.getEntity().getWorld()));
            }
        }
    }

    /**
     * Checks how to respond to an attack
     * @param e - event
     * @param damager - entity doing the damaging
     * @param flag - flag
     */
    private void respond(Cancellable e, Entity damager, Entity hurtEntity, Flag flag) {
        // Get the attacker
        if (damager instanceof Player) {
            User user = User.getInstance(damager);
            if (!setUser(user).checkIsland((Event)e, damager.getLocation(), flag)) {
                user.notify(Flags.PVP_OVERWORLD.getHintReference());
                e.setCancelled(true);
            }
        } else if (damager instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) damager;
            if (p.getShooter() instanceof Player) {
                // Allow self damage
                if (hurtEntity.equals(p.getShooter())) {
                    return;
                }
                User user = User.getInstance((Player)p.getShooter());
                if (!setUser(user).checkIsland((Event)e, damager.getLocation(), flag)) {
                    damager.setFireTicks(0);
                    damager.remove();
                    user.notify(Flags.PVP_OVERWORLD.getHintReference());
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() instanceof Player && getPlugin().getIWM().inWorld(e.getCaught().getLocation())) {
            // Allow self-inflicted damage
            if (e.getCaught().equals(e.getPlayer())) {
                return;
            }
            // Protect visitors
            if (protectedVisitor((Player)e.getCaught())) {
                User.getInstance(e.getPlayer()).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                e.setCancelled(true);
            } else if (!checkIsland(e, e.getCaught().getLocation(), getFlag(e.getCaught().getWorld()))) {
                e.getHook().remove();
                User.getInstance(e.getPlayer()).notify(Flags.PVP_OVERWORLD.getHintReference());
                e.setCancelled(true);
            }
        }
    }

    /**
     * Checks for splash damage. Remove damage if it should not affect.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSplashPotionSplash(final PotionSplashEvent e) {
        if (e.getEntity().getShooter() instanceof Player && getPlugin().getIWM().inWorld(e.getEntity().getLocation())) {
            User user = User.getInstance((Player)e.getEntity().getShooter());
            // Run through affected entities and cancel the splash if any are a protected player
            e.setCancelled(e.getAffectedEntities().stream().anyMatch(le -> blockPVP(user, le, e, getFlag(e.getEntity().getWorld()))));
        }
    }

    /**
     * Check if PVP should be blocked or not
     * @param user - user who is initiating the action
     * @param le - Living entity involed
     * @param e - event driving
     * @param flag - flag to check
     * @return true if PVP should be blocked otherwise false
     */
    private boolean blockPVP(User user, LivingEntity le, Event e, Flag flag) {
        // Check for self-inflicted damage
        if (user.getPlayer().equals(le)) {
            return false;
        }
        if (le instanceof Player) {
            // Protect visitors
            if (protectedVisitor(le)) {
                user.notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                return true;
            }
            // Check if PVP is allowed or not
            if (!checkIsland(e, le.getLocation(), flag)) {
                user.notify(Flags.PVP_OVERWORLD.getHintReference());
                return true;
            }
        }
        return false;
    }

    private boolean protectedVisitor(LivingEntity entity) {
        return getPlugin().getIWM().getIvSettings(entity.getWorld()).contains(DamageCause.ENTITY_ATTACK.name())
                && !getIslands().userIsOnIsland(entity.getWorld(), User.getInstance(entity));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionSplash(final LingeringPotionSplashEvent e) {
        // Try to get the shooter
        if (e.getEntity().getShooter() instanceof Player && getPlugin().getIWM().inWorld(e.getEntity().getLocation())) {
            // Store it and remove it when the effect is gone (Entity ID, UUID of throwing player)
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), ((Player)e.getEntity().getShooter()).getUniqueId());
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> thrownPotions.remove(e.getAreaEffectCloud().getEntityId()), e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLingeringPotionDamage(AreaEffectCloudApplyEvent e) {
        if (e.getEntity() != null && thrownPotions.containsKey(e.getEntity().getEntityId())) {
            User user = User.getInstance(thrownPotions.get(e.getEntity().getEntityId()));
            // Run through affected entities and delete them if they are safe
            e.getAffectedEntities().removeIf(le -> !le.getUniqueId().equals(user.getUniqueId()) && blockPVP(user, le, e, getFlag(e.getEntity().getWorld())));
        }
    }

    private Flag getFlag(World w) {
        switch (w.getEnvironment()) {
        case NETHER:
            return Flags.PVP_NETHER;
        case THE_END:
            return Flags.PVP_END;
        default:
            return Flags.PVP_OVERWORLD;
        }
    }

}
