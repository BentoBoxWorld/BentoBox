package us.tastybento.bskyblock.listeners.flags;

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

import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.lists.Flags;

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
            // Protect visitors
            if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && protectedVisitor((Player)e.getEntity())) {
                if (e.getDamager() instanceof Player) {
                    User.getInstance(e.getDamager()).sendMessage("protection.flags.INVINCIBLE_VISITORS.visitors-protected");
                } else if (e.getDamager() instanceof Projectile && ((Projectile)e.getDamager()).getShooter() instanceof Player) {
                    User.getInstance((Player)((Projectile)e.getDamager()).getShooter()).sendMessage("protection.flags.INVINCIBLE_VISITORS.visitors-protected");
                }
                e.setCancelled(true);
            } else {
                // PVP check
                respond(e, e.getDamager(), getFlag(e.getEntity().getWorld()));
            }
        }
    }

    /**
     * Checks how to respond to an attack
     * @param e
     * @param damager
     * @param flag
     */
    private void respond(Cancellable e, Entity damager, Flag flag) {
        // Get the attacker
        if (damager instanceof Player) {
            User user = User.getInstance(damager);
            if (!setUser(user).checkIsland((Event)e, damager.getLocation(), flag)) {
                user.sendMessage("protection.flags.PVP_OVERWORLD.pvp-not-allowed");
                e.setCancelled(true);
            }
        } else if (damager instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) damager;
            if (p.getShooter() instanceof Player) {
                User user = User.getInstance((Player)p.getShooter());
                if (!setUser(user).checkIsland((Event)e, damager.getLocation(), flag)) {
                    damager.setFireTicks(0);
                    damager.remove();
                    user.sendMessage("protection.flags.PVP_OVERWORLD.pvp-not-allowed");
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() instanceof Player && getPlugin().getIWM().inWorld(e.getCaught().getLocation())) {
            // Protect visitors
            if (protectedVisitor((Player)e.getCaught())) {
                User.getInstance(e.getPlayer()).sendMessage("protection.flags.INVINCIBLE_VISITORS.visitors-protected");
                e.setCancelled(true);
            } else if (!checkIsland(e, e.getCaught().getLocation(), getFlag(e.getCaught().getWorld()))) {
                e.getHook().remove();
                User.getInstance(e.getPlayer()).sendMessage("protection.flags.PVP_OVERWORLD.pvp-not-allowed");
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
    
    private boolean blockPVP(User user, LivingEntity le, Event e, Flag flag) {
        if (le instanceof Player) {
            // Protect visitors
            if (protectedVisitor(le)) {
                user.sendMessage("protection.flags.INVINCIBLE_VISITORS.visitors-protected");
                return true;
            }
            // Check if PVP is allowed or not
            if (!checkIsland(e, le.getLocation(), flag)) {
                user.sendMessage("protection.flags.PVP_OVERWORLD.pvp-not-allowed");
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
        return w.getEnvironment().equals(World.Environment.NORMAL) ? Flags.PVP_OVERWORLD 
                : w.getEnvironment().equals(World.Environment.NETHER) ? Flags.PVP_NETHER : Flags.PVP_END;
    }

}
