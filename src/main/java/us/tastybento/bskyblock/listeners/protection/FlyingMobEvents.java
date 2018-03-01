package us.tastybento.bskyblock.listeners.protection;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

/**
 * This class manages flying mobs. If they exist the spawned island's limits they will be removed.
 *
 * @author tastybento
 *
 */
public class FlyingMobEvents implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = false;
    private WeakHashMap<Entity, Island> mobSpawnInfo;

    /**
     * @param plugin - BSkyBlock plugin object
     */
    public FlyingMobEvents(BSkyBlock plugin) {
        this.plugin = plugin;
        mobSpawnInfo = new WeakHashMap<>();

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            //Bukkit.getLogger().info("DEBUG: checking - mobspawn size = " + mobSpawnInfo.size());
            Iterator<Entry<Entity, Island>> it = mobSpawnInfo.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Entity, Island> entry = it.next();
                if (entry.getKey() == null) {
                    //Bukkit.getLogger().info("DEBUG: removing null entity");
                    it.remove();
                } else {
                    if (entry.getKey() instanceof LivingEntity) {
                        if (!entry.getValue().inIslandSpace(entry.getKey().getLocation())) {
                            //Bukkit.getLogger().info("DEBUG: removing entity outside of island");
                            it.remove();
                            // Kill mob
                            LivingEntity mob = (LivingEntity)entry.getKey();
                            mob.setHealth(0);
                            entry.getKey().remove();
                        } else {
                            //Bukkit.getLogger().info("DEBUG: entity " + entry.getKey().getName() + " is in island space");
                        }
                    } else {
                        // Not living entity
                        it.remove();
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * Track where the mob was created. This will determine its allowable movement zone.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void mobSpawn(CreatureSpawnEvent e) {
        // Only cover withers in the island world
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        if (!e.getEntityType().equals(EntityType.WITHER) && !e.getEntityType().equals(EntityType.BLAZE) && !e.getEntityType().equals(EntityType.GHAST)) {
            return;
        }
        if (DEBUG) {
            plugin.getLogger().info("Flying mobs " + e.getEventName());
        }
        // Store where this mob originated
        plugin.getIslands().getIslandAt(e.getLocation()).ifPresent(island->mobSpawnInfo.put(e.getEntity(),island));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void mobExplosion(EntityExplodeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover in the island world
        if (e.getEntity() == null || !Util.inWorld(e.getEntity())) {
            return;
        }
        if (mobSpawnInfo.containsKey(e.getEntity())) {
            // We know about this mob
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: We know about this mob");
            }
            if (!mobSpawnInfo.get(e.getEntity()).inIslandSpace(e.getLocation())) {
                // Cancel the explosion and block damage
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: cancel flying mob explosion");
                }
                e.blockList().clear();
                e.setCancelled(true);
            }
        }
    }

    /**
     * Deal with pre-explosions
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void witherExplode(ExplosionPrimeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (!Util.inWorld(e.getEntity()) || e.getEntity() == null) {
            return;
        }
        // The wither or wither skulls can both blow up
        if (e.getEntityType() == EntityType.WITHER) {
            //plugin.getLogger().info("DEBUG: Wither");
            // Check the location
            if (mobSpawnInfo.containsKey(e.getEntity())) {
                // We know about this wither
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: We know about this wither");
                }
                if (!mobSpawnInfo.get(e.getEntity()).inIslandSpace(e.getEntity().getLocation())) {
                    // Cancel the explosion
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: cancelling wither pre-explosion");
                    }
                    e.setCancelled(true);
                }
            }
            // Testing only e.setCancelled(true);
        }
        if (e.getEntityType() == EntityType.WITHER_SKULL) {
            //plugin.getLogger().info("DEBUG: Wither skull");
            // Get shooter
            Projectile projectile = (Projectile)e.getEntity();
            if (projectile.getShooter() instanceof Wither) {
                //plugin.getLogger().info("DEBUG: shooter is wither");
                Wither wither = (Wither)projectile.getShooter();
                // Check the location
                if (mobSpawnInfo.containsKey(wither)) {
                    // We know about this wither
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: We know about this wither");
                    }
                    if (!mobSpawnInfo.get(wither).inIslandSpace(e.getEntity().getLocation())) {
                        // Cancel the explosion
                        if (DEBUG) {
                            plugin.getLogger().info("DEBUG: cancel wither skull explosion");
                        }
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    /**
     * Withers change blocks to air after they are hit (don't know why)
     * This prevents this when the wither has been spawned by a visitor
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void witherChangeBlocks(EntityChangeBlockEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Only cover withers in the island world
        if (e.getEntityType() != EntityType.WITHER || !Util.inWorld(e.getEntity()) ) {
            return;
        }
        if (mobSpawnInfo.containsKey(e.getEntity())) {
            // We know about this wither
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: We know about this wither");
            }
            if (!mobSpawnInfo.get(e.getEntity()).inIslandSpace(e.getEntity().getLocation())) {
                // Cancel the block changes
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: cancelled wither block change");
                }
                e.setCancelled(true);
            }
        }
    }

    /**
     * Clean up the hashmap. It's probably not needed, but just in case.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void mobDeath(EntityDeathEvent e) {
        mobSpawnInfo.remove(e.getEntity());
    }
}