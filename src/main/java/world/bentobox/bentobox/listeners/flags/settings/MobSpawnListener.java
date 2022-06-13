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
public class MobSpawnListener extends FlagListener
{
    /**
     * Prevents mobs spawning naturally
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawnEvent(CreatureSpawnEvent e)
    {
        this.onMobSpawn(e);
    }


    /**
     * Prevents mobs spawning naturally
     * @param e - event
     */
    void onMobSpawn(CreatureSpawnEvent e)
    {
        // If not in the right world, or spawning is not natural return
        if (!this.getIWM().inWorld(e.getEntity().getLocation()))
        {
            return;
        }

        switch (e.getSpawnReason())
        {
            // Natural
            case DEFAULT, DROWNED, JOCKEY, LIGHTNING, MOUNT, NATURAL, NETHER_PORTAL, OCELOT_BABY, PATROL,
                RAID, REINFORCEMENTS, SILVERFISH_BLOCK, TRAP, VILLAGE_DEFENSE, VILLAGE_INVASION ->
            {
                boolean cancelNatural = this.shouldCancel(e.getEntity(),
                    e.getLocation(),
                    Flags.ANIMAL_NATURAL_SPAWN,
                    Flags.MONSTER_NATURAL_SPAWN);
                e.setCancelled(cancelNatural);
            }
            // Spawners
            case SPAWNER ->
            {
                boolean cancelSpawners = this.shouldCancel(e.getEntity(),
                    e.getLocation(),
                    Flags.ANIMAL_SPAWNERS_SPAWN,
                    Flags.MONSTER_SPAWNERS_SPAWN);
                e.setCancelled(cancelSpawners);
            }
        }
    }


    /**
     * This method checks if entity should be cancelled from spawning in given location base on flag values.
     * @param entity Entity that is checked.
     * @param loc location where entity is spawned.
     * @param animalSpawnFlag Animal Spawn Flag.
     * @param monsterSpawnFlag Monster Spawn Flag.
     * @return {@code true} if flag prevents entity to spawn, {@code false} otherwise.
     */
    private boolean shouldCancel(Entity entity, Location loc, Flag animalSpawnFlag, Flag monsterSpawnFlag)
    {
        Optional<Island> island = getIslands().getIslandAt(loc);

        if (Util.isHostileEntity(entity) && !(entity instanceof PufferFish))
        {
            return island.map(i -> !i.isAllowed(monsterSpawnFlag)).
                orElseGet(() -> !monsterSpawnFlag.isSetForWorld(entity.getWorld()));
        }
        else if (Util.isPassiveEntity(entity) || entity instanceof PufferFish)
        {
            return island.map(i -> !i.isAllowed(animalSpawnFlag)).
                orElseGet(() -> !animalSpawnFlag.isSetForWorld(entity.getWorld()));
        }
        else
        {
            return false;
        }
    }
}