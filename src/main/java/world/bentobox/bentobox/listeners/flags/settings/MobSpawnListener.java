package world.bentobox.bentobox.listeners.flags.settings;

import java.util.Optional;

import org.bukkit.entity.PufferFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;


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
        // If not in the right world, or spawning is not natural return
        if (!getIWM().inWorld(e.getEntity().getLocation())) {
            return false;
        }
        CreatureSpawnEvent.SpawnReason spawnReason = e.getSpawnReason();// Natural
        if (spawnReason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || spawnReason == CreatureSpawnEvent.SpawnReason.DROWNED
                || spawnReason == CreatureSpawnEvent.SpawnReason.JOCKEY
                || spawnReason == CreatureSpawnEvent.SpawnReason.LIGHTNING
                || spawnReason == CreatureSpawnEvent.SpawnReason.MOUNT
                || spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL
                || spawnReason == CreatureSpawnEvent.SpawnReason.NETHER_PORTAL
                || spawnReason == CreatureSpawnEvent.SpawnReason.OCELOT_BABY
                || spawnReason.name().equals("PATROL")
                || spawnReason.name().equals("RAID")
                || spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
                || spawnReason == CreatureSpawnEvent.SpawnReason.SILVERFISH_BLOCK
                || spawnReason == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT
                || spawnReason == CreatureSpawnEvent.SpawnReason.TRAP
                || spawnReason == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                || spawnReason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION) {// Deal with natural spawning
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
            return false;
        }
        return false;
    }

}
