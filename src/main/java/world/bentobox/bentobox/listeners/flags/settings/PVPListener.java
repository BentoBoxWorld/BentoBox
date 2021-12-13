package world.bentobox.bentobox.listeners.flags.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.flags.FlagSettingChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles PVP
 * @author tastybento
 *
 */
public class PVPListener extends FlagListener {

    private final Map<Integer, UUID> thrownPotions = new HashMap<>();
    private final Map<Entity, Player> firedFireworks = new WeakHashMap<>();

    /**
     * This method protects players from PVP if it is not allowed and from
     * arrows fired by other players
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && getPlugin().getIWM().inWorld(e.getEntity().getWorld())) {
            // Allow self damage or NPC attack because Citizens handles its own PVP
            if (e.getEntity().equals(e.getDamager()) || e.getEntity().hasMetadata("NPC")) {
                return;
            }
            // Is PVP allowed here?
            if (this.PVPAllowed(e.getEntity().getLocation())) {
                return;
            }
            // Protect visitors
            if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && protectedVisitor((Player)e.getEntity())) {
                if (e.getDamager() instanceof Player p && p != null) {
                    User.getInstance(p).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player sh && sh != null) {
                    User.getInstance(sh).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
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
            if (!checkIsland((Event)e, (Player)damager, damager.getLocation(), flag)) {
                user.notify(getFlag(damager.getWorld()).getHintReference());
                e.setCancelled(true);
            }
        } else if (damager instanceof Projectile p && ((Projectile)damager).getShooter() instanceof Player shooter) {
            // Find out who fired the arrow
            processDamage(e, damager, shooter, hurtEntity, flag);
        } else if (damager instanceof Firework && firedFireworks.containsKey(damager)) {
            Player shooter = firedFireworks.get(damager);
            processDamage(e, damager, shooter, hurtEntity, flag);
        }
    }

    private void processDamage(Cancellable e, Entity damager, Player shooter, Entity hurtEntity, Flag flag) {
        // Allow self damage
        if (hurtEntity.equals(shooter) || shooter == null) {
            return;
        }
        User user = User.getInstance(shooter);
        if (!checkIsland((Event)e, shooter, damager.getLocation(), flag)) {
            damager.setFireTicks(0);
            hurtEntity.setFireTicks(0);
            user.notify(getFlag(damager.getWorld()).getHintReference());
            e.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if (e.getCaught() instanceof Player c && getPlugin().getIWM().inWorld(c.getLocation())) {
            // Allow self-inflicted damage or NPC damage
            if (c.equals(e.getPlayer()) || c.hasMetadata("NPC")) {
                return;
            }
            // Is PVP allowed here?
            if (this.PVPAllowed(c.getLocation())) {
                return;
            }
            // Protect visitors
            if (protectedVisitor(c)) {
                User.getInstance(e.getPlayer()).notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                e.setCancelled(true);
            } else if (!checkIsland(e, e.getPlayer(), c.getLocation(), getFlag(c.getWorld()))) {
                e.getHook().remove();
                User.getInstance(e.getPlayer()).notify(getFlag(c.getWorld()).getHintReference());
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
        if (e.getEntity().getShooter() instanceof Player p && p != null && getPlugin().getIWM().inWorld(e.getEntity().getWorld())) {
            User user = User.getInstance(p);
            // Is PVP allowed here?
            if (this.PVPAllowed(e.getEntity().getLocation())) {
                return;
            }
            // Run through affected entities and cancel the splash for protected players
            for (LivingEntity le : e.getAffectedEntities()) {
                if (!le.getUniqueId().equals(user.getUniqueId()) && blockPVP(user, le, e, getFlag(e.getEntity().getWorld()))) {
                    e.setIntensity(le, 0);
                }
            }
        }
    }

    /**
     * Check if PVP should be blocked or not
     * @param user - user who is initiating the action
     * @param le - Living entity involved
     * @param e - event driving
     * @param flag - flag to check
     * @return true if PVP should be blocked otherwise false
     */
    private boolean blockPVP(User user, LivingEntity le, Event e, Flag flag) {
        // Check for self-inflicted damage or Citizen NPCs
        if (le.equals(user.getPlayer()) || le.hasMetadata("NPC")) {
            return false;
        }
        if (le instanceof Player) {
            // Protect visitors
            if (protectedVisitor(le)) {
                user.notify(Flags.INVINCIBLE_VISITORS.getHintReference());
                return true;
            }
            // Check if PVP is allowed or not
            if (!checkIsland(e, user.getPlayer(), le.getLocation(), flag)) {
                user.notify(getFlag(le.getWorld()).getHintReference());
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
        if (e.getEntity().getShooter() instanceof Player && getPlugin().getIWM().inWorld(e.getEntity().getWorld())) {
            // Store it and remove it when the effect is gone (Entity ID, UUID of throwing player)
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), ((Player)e.getEntity().getShooter()).getUniqueId());
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> thrownPotions.remove(e.getAreaEffectCloud().getEntityId()), e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLingeringPotionDamage(AreaEffectCloudApplyEvent e) {
        if (thrownPotions.containsKey(e.getEntity().getEntityId())) {
            User user = User.getInstance(thrownPotions.get(e.getEntity().getEntityId()));
            // Run through affected entities and delete them if they are safe
            e.getAffectedEntities().removeIf(le -> !le.getUniqueId().equals(user.getUniqueId()) && blockPVP(user, le, e, getFlag(e.getEntity().getWorld())));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerShootFireworkEvent(final EntityShootBowEvent e) {
        // Only care about players shooting fireworks
        if (e.getEntity() instanceof Player && (e.getProjectile() instanceof Firework)) {
            firedFireworks.put(e.getProjectile(), (Player)e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPVPFlagToggle(final FlagSettingChangeEvent e) {
        Flag flag = e.getEditedFlag();
        // Only care about PVP Flags
        if (Flags.PVP_OVERWORLD.equals(flag) || Flags.PVP_NETHER.equals(flag) || Flags.PVP_END.equals(flag)) {
            String message = "protection.flags." + flag.getID() + "." + (e.isSetTo() ? "enabled" : "disabled");
            // Send the message to visitors
            e.getIsland().getVisitors().forEach(visitor -> User.getInstance(visitor).sendMessage(message));
            // Send the message to players on the island
            e.getIsland().getPlayersOnIsland().forEach(player -> User.getInstance(player).sendMessage(message));
        }
    }

    /**
     * Warn visitors if the island they are teleporting to has PVP on
     * @param e teleport event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) {
            return;
        }
        getIslands().getIslandAt(e.getTo()).ifPresent(island -> {
            if (island.getMemberSet(RanksManager.COOP_RANK).contains(e.getPlayer().getUniqueId())) {
                return;
            }
            if (island.isAllowed(Flags.PVP_OVERWORLD)) {
                alertUser(e.getPlayer(), Flags.PVP_OVERWORLD);
            }
            if (island.isAllowed(Flags.PVP_NETHER)) {
                alertUser(e.getPlayer(), Flags.PVP_NETHER);
            }
            if (island.isAllowed(Flags.PVP_END)) {
                alertUser(e.getPlayer(), Flags.PVP_END);
            }
        });
    }

    private void alertUser(@NonNull Player player, Flag flag) {
        String message = "protection.flags." + flag.getID() + ".enabled";
        User.getInstance(player).sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER,2F, 1F);
    }
}
