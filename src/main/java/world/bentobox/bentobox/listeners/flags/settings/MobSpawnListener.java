package world.bentobox.bentobox.listeners.flags.settings;

import org.bukkit.entity.PufferFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

import java.util.Optional;


/**
 * Handles natural mob spawning.
 * @author tastybento
 *
 */
public class MobSpawnListener extends FlagListener {

    /**
     * Prevents mobs spawning naturally
     *
     * @param e - event
     * @return true if cancelled
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onNaturalMobSpawn(CreatureSpawnEvent e) {
        // If not in the right world, return
        if (!getIWM().inWorld(e.getEntity().getLocation())) {
            return false;
        }
        // Deal with natural spawning
        if (e.getSpawnReason().equals(SpawnReason.NATURAL)
                || e.getSpawnReason().equals(SpawnReason.JOCKEY)
                || e.getSpawnReason().equals(SpawnReason.CHUNK_GEN)
                || e.getSpawnReason().equals(SpawnReason.DEFAULT)
                || e.getSpawnReason().equals(SpawnReason.MOUNT)
                || e.getSpawnReason().equals(SpawnReason.NETHER_PORTAL)) {

            Optional<Island> island = getIslands().getIslandAt(e.getLocation());
            // Cancel the event if these are true
            if (Util.isHostileEntity(e.getEntity()) && !(e.getEntity() instanceof PufferFish)) {
                boolean cancel = island.map(i -> !i.isAllowed(Flags.MONSTER_SPAWN)).orElse(!Flags.MONSTER_SPAWN.isSetForWorld(e.getEntity().getWorld()));
                e.setCancelled(cancel);
                return cancel;
            } else if (Util.isPassiveEntity(e.getEntity()) || e.getEntity() instanceof PufferFish) {
                boolean cancel = island.map(i -> !i.isAllowed(Flags.ANIMAL_SPAWN)).orElse(!Flags.ANIMAL_SPAWN.isSetForWorld(e.getEntity().getWorld()));
                e.setCancelled(cancel);
                return cancel;
            }
        }
        return false;
    }

}
