package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a team event happens.
 *
 * @author tastybento
 * @since 1.0
 */
public class TeamEvent extends IslandEvent {
    public enum TeamReason {
        INVITE,
        JOIN,
        REJECT,
        LEAVE,
        KICK,
        MAKELEADER,
        INFO,
        DELETE,
        UNKNOWN,
        UNINVITE
    };
    
    /**
     * Player involved with this event
     */
    private UUID player;    
    /**
     * True if this is an admin action
     */
    private boolean admin;
    /**
     * Reason for this event
     */
    private TeamReason reason = TeamReason.UNKNOWN;

    public TeamEvent(Island island) {
        super(island);
    }

    /**
     * @return the player involved with this event
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * @return true if this is an admin action
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @return the reason for this team event
     */
    public TeamReason getReason() {
        return reason;
    }

    /**
     * True if this is an admin driven event
     * @param admin
     * @return TeamEvent
     */
    public TeamEvent admin(boolean admin) {
        this.admin = admin;
        return this;
    }

    /**
     * @param reason for the event
     * @return
     */
    public TeamEvent reason(TeamReason reason) {
        this.reason = reason;
        return this;
    }
    
    /**
     * @param player involved in the event
     * @return
     */
    public TeamEvent involvedPlayer(UUID player) {
        this.player = player;
        return this;
    }
}
