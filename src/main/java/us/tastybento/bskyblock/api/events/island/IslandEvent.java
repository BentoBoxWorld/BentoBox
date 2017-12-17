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
public class IslandEvent {

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

    public static IslandEventBuilder builder() {
        return new IslandEventBuilder();
    }

    public static class IslandCreateEvent extends IslandBaseEvent {
        private IslandCreateEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandCreatedEvent extends IslandBaseEvent {
        private IslandCreatedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandDeleteEvent extends IslandBaseEvent {
        private IslandDeleteEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandDeletedEvent extends IslandBaseEvent {
        private IslandDeletedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandEnterEvent extends IslandBaseEvent {
        private IslandEnterEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandExitEvent extends IslandBaseEvent {
        private IslandExitEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandLockEvent extends IslandBaseEvent {
        private IslandLockEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandUnlockEvent extends IslandBaseEvent {
        private IslandUnlockEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandResetEvent extends IslandBaseEvent {
        private IslandResetEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandResettedEvent extends IslandBaseEvent {
        private IslandResettedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
    }
    public static class IslandGeneralEvent extends IslandBaseEvent {
        private IslandGeneralEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constuctor
            super(island, player, admin, location);
        }
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

        public IslandEventBuilder location(Location center) {
            this.location = center;
            return this;
        }
        
        public IslandBaseEvent build() {
            switch (reason) {
            case CREATE:
                return new IslandCreateEvent(island, player, admin, location);
            case CREATED:
                return new IslandCreatedEvent(island, player, admin, location);
            case DELETE:
                return new IslandDeleteEvent(island, player, admin, location);
            case DELETED:
                return new IslandDeletedEvent(island, player, admin, location);
            case ENTER:
                return new IslandEnterEvent(island, player, admin, location);
            case EXIT:
                return new IslandExitEvent(island, player, admin, location);
            case LOCK:
                return new IslandLockEvent(island, player, admin, location);
            case RESET:
                return new IslandResetEvent(island, player, admin, location);
            case RESETTED:
                return new IslandResettedEvent(island, player, admin, location);
            case UNLOCK:
                return new IslandUnlockEvent(island, player, admin, location);
            default:
                return new IslandGeneralEvent(island, player, admin, location);

            }
        }
    }
}
