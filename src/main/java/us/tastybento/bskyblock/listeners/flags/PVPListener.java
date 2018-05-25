/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.potion.PotionEffect;

import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.lists.Flags;

/**
 * TODO: PVP is different to other flags - it's either allowed for everyone or not allowed for everyone. Currently owners can hit visitors.
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
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            Flag flag = Flags.PVP_OVERWORLD;
            if (e.getEntity().getWorld().equals(getPlugin().getIWM().getNetherWorld())) {
                flag = Flags.PVP_NETHER;
            } else if (e.getEntity().getWorld().equals(getPlugin().getIWM().getEndWorld())) {
                flag = Flags.PVP_END;
            }
            respond(e, e.getDamager(), flag);
        }
    }

    private void respond(Event event, Entity damager, Flag flag) {
        // Get the attacker
        if (damager instanceof Player) {
            setUser(User.getInstance(damager)).checkIsland(event, damager.getLocation(), flag);
        } else if (damager instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) damager;
            if (p.getShooter() instanceof Player) {
                if (!setUser(User.getInstance((Player)p.getShooter())).checkIsland(event, damager.getLocation(), flag)) {
                    damager.setFireTicks(0);
                    damager.remove();
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() != null && e.getCaught() instanceof Player) {
            Flag flag = Flags.PVP_OVERWORLD;
            if (e.getCaught().getWorld().equals(getPlugin().getIWM().getNetherWorld())) {
                flag = Flags.PVP_NETHER;
            } else if (e.getCaught().getWorld().equals(getPlugin().getIWM().getEndWorld())) {
                flag = Flags.PVP_END;
            }
            if (checkIsland(e, e.getCaught().getLocation(), flag)) {
                e.getHook().remove();
            }
        }
    }

    /**
     * Checks for splash damage. Remove damage if it should not affect.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSplashPotionSplash(final PotionSplashEvent e) {
        // Deduce the world
        Flag flag = Flags.PVP_OVERWORLD;
        if (e.getPotion().getWorld().equals(getPlugin().getIWM().getNetherWorld())) {
            flag = Flags.PVP_NETHER;
        } else if (e.getPotion().getWorld().equals(getPlugin().getIWM().getEndWorld())) {
            flag = Flags.PVP_END;
        }

        // Try to get the thrower
        Projectile projectile = e.getEntity();
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            Player attacker = (Player)projectile.getShooter();
            // Run through all the affected entities
            for (LivingEntity entity: e.getAffectedEntities()) {
                // Self damage
                if (attacker.equals(entity)) {
                    continue;
                }
                // PVP?
                if (entity instanceof Player) {
                    if (!setUser(User.getInstance(attacker)).checkIsland(e, entity.getLocation(), flag)) {
                        for (PotionEffect effect : e.getPotion().getEffects()) {
                            entity.removePotionEffect(effect.getType());
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionSplash(final LingeringPotionSplashEvent e) {
        // Try to get the shooter
        Projectile projectile = e.getEntity();
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            UUID uuid = ((Player) projectile.getShooter()).getUniqueId();
            // Store it and remove it when the effect is gone
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), uuid);
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> thrownPotions.remove(e.getAreaEffectCloud().getEntityId()), e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionDamage(final EntityDamageByEntityEvent e) {
        if (e.getEntity() == null || e.getEntity().getUniqueId() == null) {
            return;
        }

        if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && thrownPotions.containsKey(e.getDamager().getEntityId())) {
            // Deduce the world
            Flag flag = Flags.PVP_OVERWORLD;
            if (e.getEntity().getWorld().equals(getPlugin().getIWM().getNetherWorld())) {
                flag = Flags.PVP_NETHER;
            } else if (e.getEntity().getWorld().equals(getPlugin().getIWM().getEndWorld())) {
                flag = Flags.PVP_END;
            }

            UUID attacker = thrownPotions.get(e.getDamager().getEntityId());
            // Self damage
            if (attacker.equals(e.getEntity().getUniqueId())) {
                return;
            }
            Entity entity = e.getEntity();
            // PVP?
            if (entity instanceof Player) {
                checkIsland(e, entity.getLocation(), flag);
            }
        }
    }
}
