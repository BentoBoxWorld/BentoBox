package world.bentobox.bentobox.listeners.flags;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Villager;
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

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

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
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        // Mobs being hurt
        if (e.getEntity() instanceof Animals || e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman) {
            respond(e, e.getDamager(), Flags.HURT_ANIMALS);
        } else if (e.getEntity() instanceof Villager) {
            respond(e, e.getDamager(), Flags.HURT_VILLAGERS);
        } else if (e.getEntity() instanceof Monster || e.getEntity() instanceof Squid || e.getEntity() instanceof Slime) {
            respond(e, e.getDamager(), Flags.HURT_MONSTERS);
        }
    }

    /**
     * Finds the true attacker, even if the attack was via a projectile
     * @param e - event
     * @param damager - damager
     * @param flag - flag
     */
    private void respond(EntityDamageByEntityEvent e, Entity damager, Flag flag) {
        // Get the attacker
        if (damager instanceof Player) {
            setUser(User.getInstance(damager)).checkIsland(e, damager.getLocation(), flag);
        } else if (damager instanceof Projectile) {
            // Find out who fired the projectile
            Projectile p = (Projectile) damager;
            if (p.getShooter() instanceof Player && !setUser(User.getInstance((Player)p.getShooter())).checkIsland(e, damager.getLocation(), flag)) {
                e.getEntity().setFireTicks(0);
                damager.remove();
            }
        }
    }

    /**
     * Handle attacks with a fishing rod
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() == null) {
            return;
        }

        if (((e.getCaught() instanceof Animals || e.getCaught() instanceof IronGolem || e.getCaught() instanceof Snowman) && checkIsland(e, e.getCaught().getLocation(), Flags.HURT_ANIMALS)) 
                || ((e.getCaught() instanceof Monster || e.getCaught() instanceof Squid || e.getCaught() instanceof Slime) && checkIsland(e, e.getCaught().getLocation(), Flags.HURT_MONSTERS))
                || (e.getCaught() instanceof Villager && checkIsland(e, e.getCaught().getLocation(), Flags.HURT_VILLAGERS))) {
            e.getHook().remove();
        }
    }

    /**
     * Handles feeding cookies to parrots, which may hurt them
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFeedParrots(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Parrot
                && (e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.COOKIE))
                || (e.getHand().equals(EquipmentSlot.OFF_HAND) && e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.COOKIE))) {
            checkIsland(e, e.getRightClicked().getLocation(), Flags.HURT_ANIMALS);
        }
    }

    /**
     * Checks for splash damage. Remove damage if it should not affect.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSplashPotionSplash(final PotionSplashEvent e) {
        // Try to get the shooter
        Projectile projectile = e.getEntity();
        if (projectile.getShooter() instanceof Player) {
            Player attacker = (Player)projectile.getShooter();
            // Run through all the affected entities
            for (LivingEntity entity: e.getAffectedEntities()) {
                // Self damage
                if (attacker.equals(entity)) {
                    continue;
                }
                // Monsters being hurt
                if ((entity instanceof Monster || entity instanceof Slime || entity instanceof Squid)
                        && !setUser(User.getInstance(attacker)).checkIsland(e, entity.getLocation(), Flags.HURT_MONSTERS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }

                // Mobs being hurt
                if ((entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman)
                        && !checkIsland(e, entity.getLocation(), Flags.HURT_ANIMALS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }

                // Villagers being hurt
                if (entity instanceof Villager && !checkIsland(e, entity.getLocation(), Flags.HURT_VILLAGERS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }
            }
        }
    }

    /**
     * Handle lingering potions. This tracks when a potion has been initially splashed.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionSplash(final LingeringPotionSplashEvent e) {
        // Try to get the shooter
        Projectile projectile = e.getEntity();
        if (projectile.getShooter() instanceof Player) {
            UUID uuid = ((Player)projectile.getShooter()).getUniqueId();
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
            if (entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman) {
                checkIsland(e, entity.getLocation(), Flags.HURT_ANIMALS);
            }
            // Villagers being hurt
            if (entity instanceof Villager) {
                checkIsland(e, entity.getLocation(), Flags.HURT_VILLAGERS);
            }
        }
    }
}
