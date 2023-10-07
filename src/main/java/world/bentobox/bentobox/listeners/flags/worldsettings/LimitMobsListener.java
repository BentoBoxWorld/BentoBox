package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import world.bentobox.bentobox.api.flags.FlagListener;

/**
 * Limit what mob types can spawn globally
 * @author tastybento
 *
 */
public class LimitMobsListener extends FlagListener {

    /**
     * Limit mob types if they are not allowed
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent e) {
        check(e, e.getEntityType());
        if (e.getSpawnReason().equals(SpawnReason.JOCKEY) ) {
            e.getEntity().getPassengers().forEach(pass -> check(e, pass.getType()));
        }
    }

    private void check(CreatureSpawnEvent e, EntityType type) {
        if (getIWM().inWorld(e.getLocation()) && getIWM().getMobLimitSettings(e.getLocation().getWorld()).contains(type.name())) {
            e.setCancelled(true);
        }
    }
}
