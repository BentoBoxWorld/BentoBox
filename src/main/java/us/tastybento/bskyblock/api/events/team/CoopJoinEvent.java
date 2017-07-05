package us.tastybento.bskyblock.api.events.team;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player joins an island team as a coop member
 * 
 * @author tastybento
 * @since 1.0
 */
public class CoopJoinEvent extends IslandEvent {
    private final UUID player, inviter;

    /**
     * @param island
     * @param player
     * @param inviter
     */
    public CoopJoinEvent(Island island, UUID player, UUID inviter) {
        super(island);
        this.player = player;
        this.inviter = inviter;
    }
    
    /**
     * The UUID of the player who were coop'd
     * @return the coop'd
     */
    public UUID getPlayer() {
        return player;
    }
    
    /**
     * The UUID of the player who invited the player to join the island
     * @return the inviter
     */
    public UUID getInviter() {
        return inviter;
    }
}
