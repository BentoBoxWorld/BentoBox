package world.bentobox.bentobox.managers.island;

import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.api.user.User;

/**
 * Determines the locations for new islands
 * @author tastybento, leonardochaia
 * @since 1.8.0
 *
 */
public interface NewIslandLocationStrategy {
    
    /**
     * Provide the next location for an island based on the world
     * @param world world
     * @return location of island
     */
    Location getNextLocation(World world);
    
    /**
     * Provide the next location for an island based on the world and the user doing the request.
     * This allows the user to have an effect on the location, if required. Default location is the same
     * as {@link #getNextLocation(World)}
     * @param world - world
     * @param user - user
     * @return location of island
     * @since 3.8.1
     */
    default Location getNextLocation(World world, User user) {
        return this.getNextLocation(world);
    }
}
