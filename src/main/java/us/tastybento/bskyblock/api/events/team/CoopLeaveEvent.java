package us.tastybento.bskyblock.api.events.team;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player leaves an island coop
 * 
 * @author tastybento
 * @since 1.0
 */
public class CoopLeaveEvent extends IslandEvent {
    private final UUID player, expeller;

    /**
     * Note that not all coop leaving events can be cancelled because they could be due to bigger events than
     * coop, e.g., an island being reset.
     * @param island
     * @param player
     * @param expeller
     */
    public CoopLeaveEvent(Island island, UUID player, UUID expeller) {
        super(island);
        this.player = player;
        this.expeller = expeller;
    }
    
    /**
     * The UUID of the player who left
     * @return the player who left the coop
     */
    public UUID getPlayer() {
        return player;
    }
    
    /**
     * @return the expelling player
     */
    public UUID getExpeller() {
        return expeller;
    }
}
