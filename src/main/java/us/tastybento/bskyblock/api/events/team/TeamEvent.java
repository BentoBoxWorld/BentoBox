package us.tastybento.bskyblock.api.events.team;

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
    private final UUID player;    
    /**
     * True if this is an admin action
     */
    private final boolean admin;
    /**
     * Reason for this event
     */
    private final TeamReason reason;

    private TeamEvent(Island island, UUID player, boolean admin, TeamReason reason) {
        // Final variables have to be declared in the constuctor
        super(island);
        this.player = player;
        this.admin = admin;
        this.reason = reason;
    }
    
    public static TeamEventBuilder builder() {
        return new TeamEventBuilder();
    }
    
    public static class TeamEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private Island island;
        private UUID player;
        private TeamReason reason = TeamReason.UNKNOWN;
        private boolean admin;
        
        public TeamEventBuilder island(Island island) {
            this.island = island;
            return this;
        }
        
        /**
         * True if this is an admin driven event
         * @param admin
         * @return TeamEvent
         */
        public TeamEventBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * @param reason for the event
         * @return
         */
        public TeamEventBuilder reason(TeamReason reason) {
            this.reason = reason;
            return this;
        }
        
        /**
         * @param player involved in the event
         * @return
         */
        public TeamEventBuilder involvedPlayer(UUID player) {
            this.player = player;
            return this;
        }
        
        public TeamEvent build() {
            return new TeamEvent(island, player, admin, reason);
        }
        
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

}
