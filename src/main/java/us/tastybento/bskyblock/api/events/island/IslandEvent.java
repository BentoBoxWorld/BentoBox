package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a team event happens.
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandEvent extends IslandBaseEvent {
    public enum Reason {
        CREATE,
        CREATED,
        DELETE,
        DELETED,
        ENTER,
        EXIT,
        LOCK,
        RESET,
        RESETTED,
        UNLOCK,
        UNKNOWN
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
    private final Reason reason;
    
    /**
     * Location related to the event
     */
    private Location location;

    private IslandEvent(Island island, UUID player, boolean admin, Reason reason, Location location) {
        // Final variables have to be declared in the constuctor
        super(island);
        this.player = player;
        this.admin = admin;
        this.reason = reason;
        this.location = location;
    }
    
    public static IslandEventBuilder builder() {
        return new IslandEventBuilder();
    }
    
    public static class IslandEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private Island island;
        private UUID player;
        private Reason reason = Reason.UNKNOWN;
        private boolean admin;
        private Location location;
        
        public IslandEventBuilder island(Island island) {
            this.island = island;
            return this;
        }
        
        /**
         * True if this is an admin driven event
         * @param admin
         * @return TeamEvent
         */
        public IslandEventBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * @param reason for the event
         * @return
         */
        public IslandEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }
        
        /**
         * @param player involved in the event
         * @return
         */
        public IslandEventBuilder involvedPlayer(UUID player) {
            this.player = player;
            return this;
        }
        
        public IslandEvent build() {
            return new IslandEvent(island, player, admin, reason, location);
        }

        public IslandEventBuilder location(Location location) {
            this.location = location;
            return this;
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
    public Reason getReason() {
        return reason;
    }

    public Location getLocation() {
        return location;
    }

}
