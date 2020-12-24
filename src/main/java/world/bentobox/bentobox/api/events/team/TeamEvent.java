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
