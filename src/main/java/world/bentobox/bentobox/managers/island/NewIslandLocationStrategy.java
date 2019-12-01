package world.bentobox.bentobox.managers.island;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Determines the locations for new islands
 * @author tastybento, leonardochaia
 * @since 1.8.0
 *
 */
public interface NewIslandLocationStrategy {
    Location getNextLocation(World world);
}
