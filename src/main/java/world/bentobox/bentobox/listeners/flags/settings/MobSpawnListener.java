package world.bentobox.bentobox.listeners.flags.settings;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PufferFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.potion.PotionEffectType;

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
     * This prevents to start a raid if mob spawning rules prevents it.
     * @param event RaidTriggerEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRaidStartEvent(RaidTriggerEvent event)
    {
        // If not in the right world exit immediately.
        if (!this.getIWM().inWorld(event.getWorld()))
        {
            return;
        }

        Optional<Island> island = getIslands().getIslandAt(event.getPlayer().getLocation());

        if (Boolean.TRUE.equals(island.map(i -> !i.isAllowed(Flags.MONSTER_NATURAL_SPAWN)).orElseGet(
                () -> !Flags.MONSTER_NATURAL_SPAWN.isSetForWorld(event.getWorld()))))
        {
            // Monster spawning is disabled on island or world. Cancel the raid.
            event.setCancelled(true);
        }
    }


    /**
     * This removes HERO_OF_THE_VILLAGE from players that cheated victory.
     * @param event RaidFinishEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRaidFinishEvent(RaidFinishEvent event)
    {
        // If not in the right world exit immediately.
        if (!this.getIWM().inWorld(event.getWorld()))
        {
            return;
        }

        Optional<Island> island = getIslands().getIslandAt(event.getRaid().getLocation());

        if (Boolean.TRUE.equals(island.map(i -> !i.isAllowed(Flags.MONSTER_NATURAL_SPAWN)).orElseGet(
                () -> !Flags.MONSTER_NATURAL_SPAWN.isSetForWorld(event.getWorld()))))
        {
            // CHEATERS. PUNISH THEM.
            event.getWinners().forEach(player ->
            {
                if (player.isOnline())
                {
                    player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                }
            });
        }
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
        default -> {
            // Nothing to do
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