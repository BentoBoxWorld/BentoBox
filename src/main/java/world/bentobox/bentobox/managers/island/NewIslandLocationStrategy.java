package world.bentobox.bentobox.managers.island;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Determines the locations for new islands
 * @author tastybento
 *
 */
public interface NewIslandLocationStrategy {
    public Location getNextLocation(World world);
}