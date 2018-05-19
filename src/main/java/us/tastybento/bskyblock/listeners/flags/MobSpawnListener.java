/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Optional;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles natural mob spawning.
 * @author tastybento
 *
 */
public class MobSpawnListener extends AbstractFlagListener {

    /**
     * Prevents mobs spawning naturally
     *
     * @param e - event
     * @return true if cancelled
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onNaturalMobSpawn(CreatureSpawnEvent e) {
        // If not in the right world, return
        if (!getIslandWorldManager().inWorld(e.getEntity().getLocation())) {
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
            if ((e.getEntity() instanceof Monster || e.getEntity() instanceof Slime)) {
                boolean cancel = island.map(i -> !i.isAllowed(Flags.MONSTER_SPAWN)).orElse(!Flags.MONSTER_SPAWN.isDefaultSetting());
                e.setCancelled(cancel);
                return cancel;
            } else if (e.getEntity() instanceof Animals) {
                boolean cancel = island.map(i -> !i.isAllowed(Flags.ANIMAL_SPAWN)).orElse(!Flags.ANIMAL_SPAWN.isDefaultSetting());
                e.setCancelled(cancel);
                return cancel;
            }
        }
        return false;
    }

}
