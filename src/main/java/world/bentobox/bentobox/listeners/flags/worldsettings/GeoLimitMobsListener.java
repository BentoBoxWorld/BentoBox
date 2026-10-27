package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import world.bentobox.bentobox.util.TaskScheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Provide geo limiting to mobs - removed them if they go outside island bounds
 * @author tastybento
 *
 */
public class GeoLimitMobsListener extends FlagListener {

    private final Map<Entity, Island> mobSpawnTracker = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Start the tracker when the plugin is loaded
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPluginReady(BentoBoxReadyEvent event) {
        // Kick off the task to remove entities that go outside island boundaries.
        // This runs every second, so avoid stream allocations
        TaskScheduler.globalTimer(getPlugin(), () -> {
            Map<Entity, Island> snapshot;
            synchronized (mobSpawnTracker) {
                snapshot = new HashMap<>(mobSpawnTracker);
            }
            snapshot.forEach((mob, island) -> TaskScheduler.entity(getPlugin(), mob, () -> {
                if (mob.isDead()) {
                    mobSpawnTracker.remove(mob);
                } else if (!island.onIsland(mob.getLocation())) {
                    mob.remove();
                    mobSpawnTracker.remove(mob);
                }
            }));
        }, 20L, 20L);
    }

    /**
     * Track where the mob was created. This will determine its allowable movement zone.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent e) {
        // Entity#getLocation allocates a new Location on every call, so fetch it once
        Location location = e.getLocation();
        if (getIWM().inWorld(location)
                && getIWM().getGeoLimitSettings(location.getWorld()).contains(e.getEntityType().name())) {
            getIslands().getIslandAt(location).ifPresent(i -> mobSpawnTracker.put(e.getEntity(), i));
        }
    }

    /**
     * Clean up the map when entity dies (does not handle entity removal)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobDeath(final EntityDeathEvent e) {
        mobSpawnTracker.remove(e.getEntity());
    }

    /**
     * Track projectiles launched from within an island so they can be geo-limited.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent e) {
        // Entity#getLocation allocates a new Location on every call, so fetch it once
        Location location = e.getEntity().getLocation();
        if (getIWM().inWorld(location)
                && getIWM().getGeoLimitSettings(location.getWorld()).contains(e.getEntityType().name())) {
            getIslands().getIslandAt(location).ifPresent(i -> mobSpawnTracker.put(e.getEntity(), i));
        }
    }

    /**
     * Deal with projectiles fired by entities
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileExplode(final ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof Projectile projectile && getIWM().inWorld(e.getEntity().getLocation())) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity shooter) {
                // Single lookup: the tracker is a weak map pruned on another thread.
                Island island = mobSpawnTracker.get(shooter);
                if (island != null && !island.onIsland(e.getEntity().getLocation())) {
                    e.getEntity().remove();
                    e.setCancelled(true);
                }
            }
        }
    }
}

