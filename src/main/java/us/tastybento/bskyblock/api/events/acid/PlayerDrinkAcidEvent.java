package us.tastybento.bskyblock.api.events.acid;

import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player drinks acid and... DIES
 * @author Poslovitch
 * @since 1.0
 */
public class PlayerDrinkAcidEvent extends IslandBaseEvent {
    private final Player player;

    public PlayerDrinkAcidEvent(Island island, Player player) {
        super(island);
        this.player = player;
    }

    /**
     * Gets the player which is getting killed by its stupid thirsty
     * @return the killed player
     */
    public Player getPlayer() {
        return player;
    }
}
