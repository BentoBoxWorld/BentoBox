package world.bentobox.bentobox.listeners.flags.protection;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;


/**
 * Handles hurting of monsters and animals directly and indirectly
 * @author tastybento
 *
 */
public class HurtingListener extends FlagListener {

    private final Map<Integer, Player> thrownPotions = new HashMap<>();
    private final Map<Entity, Entity> firedFireworks = new WeakHashMap<>();

    /**
     * Handles mob and monster protection
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e)
    {
        // Mobs being hurt
        if (Util.isTamableEntity(e.getEntity())) {
            this.respond(e, e.getDamager(), Flags.HURT_TAMED_ANIMALS);
        }
        else if (Util.isPassiveEntity(e.getEntity()))
        {
            this.respond(e, e.getDamager(), Flags.HURT_ANIMALS);
        }
        else if (e.getEntity() instanceof AbstractVillager)
        {
            this.respond(e, e.getDamager(), Flags.HURT_VILLAGERS);
        }
        else if (Util.isHostileEntity(e.getEntity()))
        {
            this.respond(e, e.getDamager(), Flags.HURT_MONSTERS);
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
        if (damager instanceof Player player) {
            checkIsland(e, player, e.getEntity().getLocation(), flag);
        } else if (damager instanceof Projectile p && // Find out who fired the projectile
                p.getShooter() instanceof Player player && !checkIsland(e, player, e.getEntity().getLocation(), flag)) {
            e.getEntity().setFireTicks(0);
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

        if ((Util.isTamableEntity(e.getCaught()) && checkIsland(e, e.getPlayer(), e.getCaught().getLocation(), Flags.HURT_TAMED_ANIMALS))
                || (Util.isPassiveEntity(e.getCaught()) && checkIsland(e, e.getPlayer(), e.getCaught().getLocation(), Flags.HURT_ANIMALS))
                || (Util.isHostileEntity(e.getCaught()) && checkIsland(e, e.getPlayer(), e.getCaught().getLocation(), Flags.HURT_MONSTERS))
                || (e.getCaught() instanceof AbstractVillager && checkIsland(e, e.getPlayer(), e.getCaught().getLocation(), Flags.HURT_VILLAGERS))) {
            e.getHook().remove();
        }

        // Handle Armor stands that can be pulled using a rod
        if (e.getCaught() instanceof ArmorStand && checkIsland(e, e.getPlayer(), e.getCaught().getLocation(), Flags.ARMOR_STAND)) {
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
            if (((Parrot) e.getRightClicked()).isTamed()) {
                checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.HURT_TAMED_ANIMALS);
            } else {
                checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.HURT_ANIMALS);
            }
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
        if (projectile.getShooter() instanceof Player attacker) {
            // Run through all the affected entities
            for (LivingEntity entity: e.getAffectedEntities()) {
                // Self damage
                if (attacker.equals(entity)) {
                    continue;
                }
                // Monsters being hurt
                if (Util.isHostileEntity(entity) && !checkIsland(e, attacker, entity.getLocation(), Flags.HURT_MONSTERS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }

                // Tamed animals being hurt
                if (Util.isTamableEntity(entity) && !checkIsland(e, attacker, entity.getLocation(), Flags.HURT_TAMED_ANIMALS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }

                // Mobs being hurt
                if (Util.isPassiveEntity(entity) && !checkIsland(e, attacker, entity.getLocation(), Flags.HURT_ANIMALS)) {
                    for (PotionEffect effect : e.getPotion().getEffects()) {
                        entity.removePotionEffect(effect.getType());
                    }
                }

                // Villagers being hurt
                if (entity instanceof AbstractVillager && !checkIsland(e, attacker, entity.getLocation(), Flags.HURT_VILLAGERS)) {
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
        if (projectile.getShooter() instanceof Player player) {
            // Store it and remove it when the effect is gone
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), player);
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> thrownPotions.remove(e.getAreaEffectCloud().getEntityId()), e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionDamage(final EntityDamageByEntityEvent e) {
        if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && thrownPotions.containsKey(e.getDamager().getEntityId())) {
            Player attacker = thrownPotions.get(e.getDamager().getEntityId());
            processDamage(e, attacker);
        }
    }

    private void processDamage(EntityDamageByEntityEvent e, Player attacker) {
        // Self damage
        if (attacker == null || attacker.equals(e.getEntity())) {
            return;
        }
        Entity entity = e.getEntity();
        // Monsters being hurt
        if (Util.isHostileEntity(entity)) {
            checkIsland(e, attacker, entity.getLocation(), Flags.HURT_MONSTERS);
        }
        // Tamed animals being hurt
        if (Util.isTamableEntity(entity)) {
            checkIsland(e, attacker, entity.getLocation(), Flags.HURT_TAMED_ANIMALS);
        }
        // Mobs being hurt
        if (Util.isPassiveEntity(entity)) {
            checkIsland(e, attacker, entity.getLocation(), Flags.HURT_ANIMALS);
        }
        // Villagers being hurt
        if (entity instanceof AbstractVillager) {
            checkIsland(e, attacker, entity.getLocation(), Flags.HURT_VILLAGERS);
        }

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onFireworkDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Firework && firedFireworks.containsKey(e.getDamager())) {
            processDamage(e, (Player)firedFireworks.get(e.getDamager()));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerShootEvent(final EntityShootBowEvent e) {
        // Only care about players shooting fireworks
        if (e.getEntityType().equals(EntityType.PLAYER) && (e.getProjectile() instanceof Firework)) {
            firedFireworks.put(e.getProjectile(), e.getEntity());
        }
    }
}
