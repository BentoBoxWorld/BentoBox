package world.bentobox.bentobox.listeners.flags.settings;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PufferFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Handles natural mob spawning.
 * @author tastybento
 */
public class MobSpawnListener extends FlagListener {

    /**
     * Prevents mobs spawning naturally
     *
     * @param e - event
     * @return true if cancelled
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onMobSpawn(CreatureSpawnEvent e) {
        // If not in the right world, or spawning is not natural return
        if (!getIWM().inWorld(e.getEntity().getLocation())) {
            return false;
        }
        switch (e.getSpawnReason()) {
        // Natural
        case DEFAULT:
        case DROWNED:
        case JOCKEY:
        case LIGHTNING:
        case MOUNT:
        case NATURAL:
        case NETHER_PORTAL:
        case OCELOT_BABY:
        case PATROL:
        case RAID:
        case REINFORCEMENTS:
        case SILVERFISH_BLOCK:
        case SLIME_SPLIT:
        case TRAP:
        case VILLAGE_DEFENSE:
        case VILLAGE_INVASION:
            boolean cancelNatural = shouldCancel(e.getEntity(), e.getLocation(), Flags.ANIMAL_NATURAL_SPAWN, Flags.MONSTER_NATURAL_SPAWN);
            e.setCancelled(cancelNatural);
            return cancelNatural;
        // Spawners
        case SPAWNER:
            boolean cancelSpawners = shouldCancel(e.getEntity(), e.getLocation(), Flags.ANIMAL_SPAWNERS_SPAWN, Flags.MONSTER_SPAWNERS_SPAWN);
            e.setCancelled(cancelSpawners);
            return cancelSpawners;
        default:
            return false;
        }
    }

    private boolean shouldCancel(Entity entity, Location loc, Flag animalSpawnFlag, Flag monsterSpawnFlag) {
        Optional<Island> island = getIslands().getIslandAt(loc);
        if (Util.isHostileEntity(entity) && !(entity instanceof PufferFish)) {
            return island.map(i -> !i.isAllowed(monsterSpawnFlag)).orElse(!monsterSpawnFlag.isSetForWorld(entity.getWorld()));
        } else if (Util.isPassiveEntity(entity) || entity instanceof PufferFish) {
            return island.map(i -> !i.isAllowed(animalSpawnFlag)).orElse(!animalSpawnFlag.isSetForWorld(entity.getWorld()));
        }
        return false;
    }

}
