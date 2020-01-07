package world.bentobox.bentobox.api.events.team;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when a team event happens.
 *
 * @author tastybento
 */
public class TeamEvent {

    public enum Reason {
        INVITE,
        JOIN,
        REJECT,
        LEAVE,
        KICK,
        SETOWNER,
        INFO,
        /**
         * The island has been reset by the owner.
         */
        DELETE,
        UNKNOWN,
        UNINVITE,
        JOINED
    }

    public static TeamEventBuilder builder() {
        return new TeamEventBuilder();
    }

    public static class TeamJoinEvent extends IslandBaseEvent {
        private TeamJoinEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Called after a player has joined an island
     * @since 1.3.0
     */
    public static class TeamJoinedEvent extends IslandBaseEvent {
        /**
         * Called after a player has joined an island
         * @param island - island
         * @param player - player
         * @param admin - whether this was due to an admin action
         * @param location - location
         * @since 1.3.0
         */
        private TeamJoinedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamInviteEvent extends IslandBaseEvent {
        private TeamInviteEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamLeaveEvent extends IslandBaseEvent {
        private TeamLeaveEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamRejectEvent extends IslandBaseEvent {
        private TeamRejectEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamKickEvent extends IslandBaseEvent {
        private TeamKickEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Event fires before a setowner is performed on an island.
     * To get the old owner, get from the island object. The new owner is the player's UUID.
     * @author tastybento
     *
     */
    public static class TeamSetownerEvent extends IslandBaseEvent {
        private TeamSetownerEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
        /**
         * Convenience method to get the old owner of the island
         * @return UUID of old owner
         */
        public UUID getOldOwner() {
            return island.getOwner();
        }
        /**
         * Convenience method to get the new owner of the island
         * @return UUID of new owner
         */
        public UUID getNewOwner() {
            return playerUUID;
        }
    }
    public static class TeamInfoEvent extends IslandBaseEvent {
        private TeamInfoEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamDeleteEvent extends IslandBaseEvent {
        private TeamDeleteEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamUninviteEvent extends IslandBaseEvent {
        private TeamUninviteEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    public static class TeamGeneralEvent extends IslandBaseEvent {
        private TeamGeneralEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }

    public static class TeamEventBuilder {
        private Island island;
        private UUID player;
        private Reason reason = Reason.UNKNOWN;
        private boolean admin;
        private Location location;

        public TeamEventBuilder island(Island island) {
            this.island = island;
            return this;
        }

        /**
         * True if this is an admin driven event
         * @param admin - true if due to an admin event
         * @return TeamEvent
         */
        public TeamEventBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * @param reason for the event
         * @return TeamEventBuilder
         */
        public TeamEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @param player - the player involved in the event
         * @return TeamEventBuilder
         */
        public TeamEventBuilder involvedPlayer(UUID player) {
            this.player = player;
            return this;
        }

        public TeamEventBuilder location(Location center) {
            location = center;
            return this;
        }

        private IslandBaseEvent getEvent() {
            switch (reason) {
            case JOIN:
                return new TeamJoinEvent(island, player, admin, location);
            case JOINED:
                return new TeamJoinedEvent(island, player, admin, location);
            case INVITE:
                return new TeamInviteEvent(island, player, admin, location);
            case LEAVE:
                return new TeamLeaveEvent(island, player, admin, location);
            case REJECT:
                return new TeamRejectEvent(island, player, admin, location);
            case KICK:
                return new TeamKickEvent(island, player, admin, location);
            case SETOWNER:
                return new TeamSetownerEvent(island, player, admin, location);
            case INFO:
                return new TeamInfoEvent(island, player, admin, location);
            case DELETE:
                return new TeamDeleteEvent(island, player, admin, location);
            case UNINVITE:
                return new TeamUninviteEvent(island, player, admin, location);
            default:
                return new TeamGeneralEvent(island, player, admin, location);
            }
        }
        
        /**
         * Build the event and call it
         * @return event
         */
        public IslandBaseEvent build() {
            IslandBaseEvent e = getEvent();
            Bukkit.getPluginManager().callEvent(e);
            return e;
        }
    }
}
