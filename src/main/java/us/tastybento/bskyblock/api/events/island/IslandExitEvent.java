package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player exits an island's protected area
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandExitEvent extends IslandEvent {
    private final UUID player;
    private final Location location;

    /**
     * @param island that the player is leaving
     * @param player
     * @param location - Location of where the player exited the island's protected area
     */
    public IslandExitEvent(Island island, UUID player, Location location) {
        super(island);
        this.player = player;
        this.location = location;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Location of where the player exited the island's protected area
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
