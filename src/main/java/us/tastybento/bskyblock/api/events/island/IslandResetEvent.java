package us.tastybento.bskyblock.api.events.island;

import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired when a player resets an island
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandResetEvent extends IslandEvent {
    private final Player player;

    /**
     * @param island
     * @param player
     */
    public IslandResetEvent(Island island, Player player) {
        super(island);
        this.player = player;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }
}
