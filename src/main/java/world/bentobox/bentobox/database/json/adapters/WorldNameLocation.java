package world.bentobox.bentobox.database.json.adapters;

import org.bukkit.Location;

/**
 * A location read from the database while its world was not loaded.
 * <p>
 * The world name is kept so that the location can still be written back (instead of failing
 * every save until the server restarts) and so the caller can see which world it belongs to.
 *
 * @since 3.23.4
 */
class WorldNameLocation extends Location {

    private final String worldName;

    WorldNameLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        super(null, x, y, z, yaw, pitch);
        this.worldName = worldName;
    }

    String getWorldName() {
        return worldName;
    }
}
