/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles hurting of monsters and animals directly and indirectly
 * @author tastybento
 *
 */
public class HurtingListener extends AbstractFlagListener {

    private HashMap<Integer, UUID> thrownPotions = new HashMap<>();


    /**
     * Handles mob and monster protection
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        // Mobs being hurt
        if (e.getEntity() instanceof Animals || e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman
                || e.getEntity() instanceof Villager) {
            respond(e, e.getDamager(), Flags.HURT_MOBS);
        } else if (e.getEntity() instanceof Monster || e.getEntity() instanceof Squid || e.getEntity() instanceof Slime) {
            respond(e, e.getDamager(), Flags.HURT_MONSTERS);
        }
    }

    /**
     * Finds the true attacker, even if the attack was via a projectile
     * @param event
     * @param damager
     * @param hurtMobs
     */
    private void respond(Event event, Entity damager, Flag hurtMobs) {
        // Get the attacker
        if (damager instanceof Player) {
            setUser(User.getInstance(damager)).checkIsland(event, damager.getLocation(), hurtMobs);
        } else if (damager instanceof Projectile) {
            // Find out who fired the projectile
            Projectile p = (Projectile) damager;
            if (p.getShooter() instanceof Player) {
                if (!setUser(User.getInstance((Player)p.getShooter())).checkIsland(event, damager.getLocation(), hurtMobs)) {
                    damager.setFireTicks(0);
                    damager.remove();
                }
            }
        }

    }

    /**
     * Handle attacks with a fishing rod
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() == null)
            return;

        if (e.getCaught() instanceof Animals || e.getCaught() instanceof IronGolem || e.getCaught() instanceof Snowman
                || e.getCaught() instanceof Villager) {
            if (checkIsland(e, e.getCaught().getLocation(), Flags.HURT_MONSTERS)) {
                e.getHook().remove();
                return;
            }
        }
        if (e.getCaught() instanceof Monster || e.getCaught() instanceof Squid || e.getCaught() instanceof Slime) {
            if (checkIsland(e, e.getCaught().getLocation(), Flags.HURT_MONSTERS)) {
                e.getHook().remove();
                return;
            }
        }
    }


    /**
     * Handles feeding cookies to animals, which may hurt them
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Animals) {
            if ((e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.COOKIE))
                    || (e.getHand().equals(EquipmentSlot.OFF_HAND) && e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.COOKIE))) {
                checkIsland(e, e.getRightClicked().getLocation(), Flags.HURT_MOBS);
            }
        }
    }

    /**
     * Checks for splash damage. Remove damage if it should not affect.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSplashPotionSplash(final PotionSplashEvent e) {
        // Try to get the shooter
        Projectile projectile = (Projectile) e.getEntity();
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            Player attacker = (Player)projectile.getShooter();
            // Run through all the affected entities
            for (LivingEntity entity: e.getAffectedEntities()) {
                // Self damage
                if (attacker.equals(entity)) {
                    continue;
                }
                // Monsters being hurt
                if (entity instanceof Monster || entity instanceof Slime || entity instanceof Squid) {
                    if (!setUser(User.getInstance(attacker)).checkIsland(e, entity.getLocation(), Flags.HURT_MONSTERS)) {
                        for (PotionEffect effect : e.getPotion().getEffects()) {
                            entity.removePotionEffect(effect.getType());
                        }
                    }
                }

                // Mobs being hurt
                if (entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman
                        || entity instanceof Villager) {
                    if (!checkIsland(e, entity.getLocation(), Flags.HURT_MONSTERS)) {
                        for (PotionEffect effect : e.getPotion().getEffects()) {
                            entity.removePotionEffect(effect.getType());
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle lingering potions. This tracks when a potion has been initially splashed.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionSplash(final LingeringPotionSplashEvent e) {
        // Try to get the shooter
        Projectile projectile = (Projectile) e.getEntity();
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            UUID uuid = ((Player)projectile.getShooter()).getUniqueId();
            // Store it and remove it when the effect is gone
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), uuid);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    thrownPotions.remove(e.getAreaEffectCloud().getEntityId());
                    }, e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionDamage(final EntityDamageByEntityEvent e) {
        if (e.getEntity() == null || e.getEntity().getUniqueId() == null) {
            return;
        }

        if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && thrownPotions.containsKey(e.getDamager().getEntityId())) {
            UUID attacker = thrownPotions.get(e.getDamager().getEntityId());
            // Self damage
            if (attacker.equals(e.getEntity().getUniqueId())) {
                return;
            }
            Entity entity = e.getEntity();
            // Monsters being hurt
            if (entity instanceof Monster || entity instanceof Slime || entity instanceof Squid) {
                checkIsland(e, entity.getLocation(), Flags.HURT_MONSTERS);
            }
            // Mobs being hurt
            if (entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman
                    || entity instanceof Villager) {
                checkIsland(e, entity.getLocation(), Flags.HURT_MONSTERS);
            }
        }
    }
}
