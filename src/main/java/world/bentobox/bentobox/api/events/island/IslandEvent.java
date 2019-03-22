package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.lists.Flags;

/**
 * Fired when an island event happens.
 *
 * @author tastybento
 */
public class IslandEvent extends IslandBaseEvent {

    private final Reason reason;

    /**
     * Fired every time an island event occurs. For developers who just want one event and will use an enum to track the reason
     * @param island - the island involved in the event
     * @param playerUUID - the player's UUID involved in the event
     * @param admin - true if this is due to an admin event
     * @param location - location of event
     * @param reason - see {@link #getReason()}
     */
    public IslandEvent(Island island, UUID playerUUID, boolean admin, Location location, Reason reason) {
        super(island, playerUUID, admin, location);
        this.reason = reason;
    }

    /**
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Reason for the event
     */
    public enum Reason {
        /**
         * Fired when a player will be banned from an island.
         * @since 1.1
         */
        BAN,
        /**
         * Called when a player has been allocated a new island spot
         * but before the island itself has been pasted or the player teleported.
         */
        CREATE,
        /**
         * Fired when an island is created for the very first time. Occurs after everything
         * has been completed.
         */
        CREATED,
        /**
         * Fired when an island is to be deleted. Note an island can be deleted without having
         * chunks removed.
         */
        DELETE,
        /**
         * Fired when island chunks are going to be deleted
         */
        DELETE_CHUNKS,
        /**
         * Fired after all island chunks have been deleted or set for regeneration by the server
         */
        DELETED,
        /**
         * Fired when a player enters an island
         */
        ENTER,
        /**
         * Fired when a player exits an island
         */
        EXIT,
        /**
         * Fired when there a player makes a change to the lock state of their island
         * To read the rank value, check the {@link Flags#LOCK} flag.
         */
        LOCK,
        /**
         * Called when a player has been reset and a new island spot allocated
         * but before the island itself has been pasted or the player teleported.
         */
        RESET,
        /**
         * Called when an island has been pasted due to a reset.
         * Occurs before the old island has been deleted but after everything else.
         * ie., island pasted, player teleported, etc.
         */
        RESETTED,
        /**
         * Fired when a player will be unbanned from an island.
         * @since 1.1
         */
        UNBAN,
        /**
         * Reserved
         */
        UNLOCK,
        /**
         * Reserved
         */
        UNKNOWN,
        /**
         * Player was unregistered from the island by admin
         * @since 1.3.0
         */
        UNREGISTERED,
        /**
         * Player was registered to the island by admin
         * @since 1.3.0
         */
        REGISTERED,
        /**
         * Player was expelled
         * @since 1.4.0
         */
        EXPEL
    }

    public static IslandEventBuilder builder() {
        return new IslandEventBuilder();
    }

    /**
     * Fired when a player will be expelled from an island.
     * May be cancelled.
     * Cancellation will result in the expel being aborted.
     *
     * @since 1.4.0
     */
    public static class IslandExpelEvent extends IslandBaseEvent {
        private IslandExpelEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }

    /**
     * Fired when a player will be banned from an island.
     * May be cancelled.
     * Cancellation will result in the ban being aborted.
     *
     * @since 1.1
     */
    public static class IslandBanEvent extends IslandBaseEvent {
        private IslandBanEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }

    /**
     * Fired when a player will be banned from an island.
     * May be cancelled.
     * Cancellation will result in the unban being aborted.
     *
     * @since 1.1
     */
    public static class IslandUnbanEvent extends IslandBaseEvent {
        public IslandUnbanEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }

    /**
     * Fired when an island is going to be created.
     * May be cancelled.
     *
     */
    public static class IslandCreateEvent extends IslandBaseEvent {
        private IslandCreateEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island is created.
     *
     */
    public static class IslandCreatedEvent extends IslandBaseEvent {
        private IslandCreatedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island is going to be deleted.
     * May be cancelled.
     *
     */
    public static class IslandDeleteEvent extends IslandBaseEvent {
        private IslandDeleteEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island chunks are going to be deleted.
     * May be cancelled.
     *
     */
    public static class IslandDeleteChunksEvent extends IslandBaseEvent {
        private final IslandDeletion deletedIslandInfo;

        private IslandDeleteChunksEvent(Island island, UUID player, boolean admin, Location location, IslandDeletion deletedIsland) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            this.deletedIslandInfo = deletedIsland;
        }

        public IslandDeletion getDeletedIslandInfo() {
            return deletedIslandInfo;
        }
    }
    /**
     * Fired when an island is deleted.
     *
     */
    public static class IslandDeletedEvent extends IslandBaseEvent {
        private final IslandDeletion deletedIslandInfo;

        private IslandDeletedEvent(Island island, UUID player, boolean admin, Location location, IslandDeletion deletedIsland) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            this.deletedIslandInfo = deletedIsland;
        }

        public IslandDeletion getDeletedIslandInfo() {
            return deletedIslandInfo;
        }
    }

    /**
     * Fired when a player is unregistered from an island.
     * @since 1.3.0
     */
    public static class IslandUnregisteredEvent extends IslandBaseEvent {
        private IslandUnregisteredEvent(Island island, UUID player, boolean admin, Location location) {
            super(island, player, admin, location);
        }
    }

    /**
     * Fired when a player is registered from an island.
     * @since 1.3.0
     */
    public static class IslandRegisteredEvent extends IslandBaseEvent {
        private IslandRegisteredEvent(Island island, UUID player, boolean admin, Location location) {
            super(island, player, admin, location);
        }
    }

    /**
     * Fired when an a player enters an island.
     * Cancellation has no effect.
     */
    public static class IslandEnterEvent extends IslandBaseEvent {
        private IslandEnterEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when a player exits an island.
     * Cancellation has no effect.
     */
    public static class IslandExitEvent extends IslandBaseEvent {
        private IslandExitEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island is locked
     *
     */
    public static class IslandLockEvent extends IslandBaseEvent {
        private IslandLockEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island is unlocked
     *
     */
    public static class IslandUnlockEvent extends IslandBaseEvent {
        private IslandUnlockEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when an island is going to be reset.
     * May be cancelled.
     */
    public static class IslandResetEvent extends IslandBaseEvent {
        private IslandResetEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired after an island is reset
     *
     */
    public static class IslandResettedEvent extends IslandBaseEvent {
        private IslandResettedEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
        }
    }
    /**
     * Fired when something happens to the island not covered by other events
     *
     */
    public static class IslandGeneralEvent extends IslandBaseEvent {
        private IslandGeneralEvent(Island island, UUID player, boolean admin, Location location) {
            // Final variables have to be declared in the constructor
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
        private IslandDeletion deletedIslandInfo;

        public IslandEventBuilder island(Island island) {
            this.island = island;
            return this;
        }

        /**
         * True if this is an admin driven event
         * @param admin - true if due to admin event
         * @return TeamEvent
         */
        public IslandEventBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * @param reason for the event
         * @return IslandEventBuilder
         */
        public IslandEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @param player - the player involved in the event
         * @return IslandEventBuilder
         */
        public IslandEventBuilder involvedPlayer(UUID player) {
            this.player = player;
            return this;
        }

        public IslandEventBuilder location(Location center) {
            location = center;
            return this;
        }

        public IslandEventBuilder deletedIslandInfo(IslandDeletion deletedIslandInfo) {
            this.deletedIslandInfo = deletedIslandInfo;
            return this;
        }

        public IslandBaseEvent build() {
            // Call the generic event for developers who just want one event and use the Reason enum
            Bukkit.getServer().getPluginManager().callEvent(new IslandEvent(island, player, admin, location, reason));
            // Generate explicit events
            switch (reason) {
            case EXPEL:
                IslandExpelEvent expel = new IslandExpelEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(expel);
                return expel;
            case BAN:
                IslandBanEvent ban = new IslandBanEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(ban);
                return ban;
            case CREATE:
                IslandCreateEvent create = new IslandCreateEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(create);
                return create;
            case CREATED:
                IslandCreatedEvent created = new IslandCreatedEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(created);
                return created;
            case DELETE:
                IslandDeleteEvent delete = new IslandDeleteEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(delete);
                return delete;
            case DELETE_CHUNKS:
                IslandDeleteChunksEvent deleteChunks = new IslandDeleteChunksEvent(island, player, admin, location, deletedIslandInfo);
                Bukkit.getServer().getPluginManager().callEvent(deleteChunks);
                return deleteChunks;
            case DELETED:
                IslandDeletedEvent deleted = new IslandDeletedEvent(island, player, admin, location, deletedIslandInfo);
                Bukkit.getServer().getPluginManager().callEvent(deleted);
                return deleted;
            case ENTER:
                IslandEnterEvent enter = new IslandEnterEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(enter);
                return enter;
            case EXIT:
                IslandExitEvent exit = new IslandExitEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(exit);
                return exit;
            case LOCK:
                IslandLockEvent lock = new IslandLockEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(lock);
                return lock;
            case RESET:
                IslandResetEvent reset = new IslandResetEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(reset);
                return reset;
            case RESETTED:
                IslandResettedEvent resetted = new IslandResettedEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(resetted);
                return resetted;
            case UNBAN:
                IslandUnbanEvent unban = new IslandUnbanEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(unban);
                return unban;
            case UNLOCK:
                IslandUnlockEvent unlock = new IslandUnlockEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(unlock);
                return unlock;
            case REGISTERED:
                IslandRegisteredEvent reg = new IslandRegisteredEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(reg);
                return reg;
            case UNREGISTERED:
                IslandUnregisteredEvent unreg = new IslandUnregisteredEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(unreg);
                return unreg;
            default:
                IslandGeneralEvent general = new IslandGeneralEvent(island, player, admin, location);
                Bukkit.getServer().getPluginManager().callEvent(general);
                return general;
            }
        }
    }
}
