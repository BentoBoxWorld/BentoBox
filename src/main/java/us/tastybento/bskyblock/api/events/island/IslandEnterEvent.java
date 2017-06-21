package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player enters an island's area
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandEnterEvent extends IslandEvent {
    private final UUID player;
    private final Location location;

    /**
     * Called to create the event
     * @param island - island the player is entering
     * @param player
     * @param location - Location of where the player entered the island or tried to enter
     */
    public IslandEnterEvent(Island island, UUID player, Location location) {
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
     * Location of where the player entered the island or tried to enter
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

}
