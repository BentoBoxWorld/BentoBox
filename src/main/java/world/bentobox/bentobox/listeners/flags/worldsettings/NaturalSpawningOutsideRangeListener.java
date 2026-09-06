package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link world.bentobox.bentobox.lists.Flags#NATURAL_SPAWNING_OUTSIDE_RANGE}.
 *
 * @author Poslovitch
 * @since 1.3.0
 */
public class NaturalSpawningOutsideRangeListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        // Entity#getLocation allocates a new Location on every call, so fetch it once
        Location location = e.getLocation();
        if (!getIWM().inWorld(location) || Flags.NATURAL_SPAWNING_OUTSIDE_RANGE.isSetForWorld(location.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        // If it is a natural spawn and there is no protected island at the location, block the spawn.
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && getIslands().getProtectedIslandAt(location).isEmpty()) {
            e.setCancelled(true);
        }
    }
}
