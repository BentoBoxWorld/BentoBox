package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
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
         * Called before an island is going to be cleared of island members.
         * This event occurs before resets or other island clearing activities.
         * Cannot be cancelled.
         * @since 1.12.0
         */
        PRECLEAR,
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
        EXPEL,
        /**
         * The island was reserved and now is being pasted.
         * @since 1.6.0
         */
        RESERVED,
        /**
         * The island protection range was changed.
         * @since 1.11.0
         */
        RANGE_CHANGE,
        /**
         * Event that will fire any time a player's rank changes on an island.
         * @since 1.13.0
         */
        RANK_CHANGE
    }

    public static IslandEventBuilder builder() {
        return new IslandEventBuilder();
    }

    /**
     * Fired when a player's rank has changed on an island.
     * Cancellation has no effect.
     * @since 1.13.0
     */
    public static class IslandRankChangeEvent extends IslandBaseEvent {

        private final int oldRank;
        private final int newRank;

        public IslandRankChangeEvent(Island island, UUID playerUUID, boolean admin, Location location, int oldRank, int newRank) {
            super(island, playerUUID, admin, location);
            this.oldRank = oldRank;
            this.newRank = newRank;
        }

        public int getOldRank() {
            return oldRank;
        }

        public int getNewRank(){
            return newRank;
        }
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
        private @NonNull BlueprintBundle blueprintBundle;

        private IslandCreateEvent(Island island, UUID player, boolean admin, Location location, @NonNull BlueprintBundle blueprintBundle) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            this.blueprintBundle = blueprintBundle;
        }

        /**
         * @since 1.6.0
         */
        @NonNull
        public BlueprintBundle getBlueprintBundle() {
            return blueprintBundle;
        }

        /**
         * @since 1.6.0
         */
        public void setBlueprintBundle(@NonNull BlueprintBundle blueprintBundle) {
            this.blueprintBundle = blueprintBundle;
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
     * Fired when island blocks are going to be deleted.
     * If canceled, the island blocks will not be deleted. Note that by the time this is called
     * the ownership of the island may have been removed. This event is just for detecting
     * that the island blocks are going to be removed.
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
     * Fired when an island is reserved for a player
     * @since 1.12.0
     */
    public static class IslandReservedEvent extends IslandBaseEvent {
        private IslandReservedEvent(Island island, UUID player, boolean admin, Location location) {
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
     * Fired before an island has its player data cleared, e.g., just before a reset
     * @since 1.12.0
     */
    public static class IslandPreclearEvent extends IslandBaseEvent {
        private final @NonNull Island oldIsland;

        private IslandPreclearEvent(Island island, UUID player, boolean admin, Location location, @NonNull Island oldIsland) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            // Create a copy of the old island
            this.oldIsland = new Island(oldIsland);
        }

        /**
         * @since 1.12.0
         */
        @NonNull
        public Island getOldIsland() {
            return oldIsland;
        }
    }

    /**
     * Fired when an island is going to be reset.
     * May be cancelled.
     */
    public static class IslandResetEvent extends IslandBaseEvent {
        private final @NonNull Island oldIsland;
        private @NonNull BlueprintBundle blueprintBundle;

        private IslandResetEvent(Island island, UUID player, boolean admin, Location location, @NonNull BlueprintBundle blueprintBundle, @NonNull Island oldIsland) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            this.blueprintBundle = blueprintBundle;
            // Create a copy of the old island
            this.oldIsland = new Island(oldIsland);
        }

        /**
         * @since 1.12.0
         */
        @NonNull
        public Island getOldIsland() {
            return oldIsland;
        }

        /**
         * @since 1.6.0
         */
        @NonNull
        public BlueprintBundle getBlueprintBundle() {
            return blueprintBundle;
        }

        /**
         * @since 1.6.0
         */
        public void setBlueprintBundle(@NonNull BlueprintBundle blueprintBundle) {
            this.blueprintBundle = blueprintBundle;
        }
    }
    /**
     * Fired after an island is reset
     *
     */
    public static class IslandResettedEvent extends IslandBaseEvent {
        private final @NonNull Island oldIsland;

        private IslandResettedEvent(Island island, UUID player, boolean admin, Location location, Island oldIsland) {
            // Final variables have to be declared in the constructor
            super(island, player, admin, location);
            // Create a copy of the old island
            this.oldIsland = new Island(oldIsland);
        }

        /**
         * @since 1.12.0
         */
        @NonNull
        public Island getOldIsland() {
            return oldIsland;
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


    /**
     * Fired when island protection range is changed.
     * @since 1.11.0
     */
    public static class IslandProtectionRangeChangeEvent extends IslandBaseEvent {
        /**
         * New protection range value.
         */
        private int newRange;

        /**
         * Old protection range value.
         */
        private int oldRange;

        /**
         * Constructor IslandProtectionRangeChange creates a new IslandProtectionRangeChange instance.
         *
         * @param island of type Island
         * @param player of type UUID
         * @param admin of type boolean
         * @param location of type Location
         * @param newRange of type int
         * @param oldRange of type int
         */
        private IslandProtectionRangeChangeEvent(Island island, UUID player, boolean admin, Location location, int newRange, int oldRange) {
            super(island, player, admin, location);
            this.newRange = newRange;
            this.oldRange = oldRange;
        }


        /**
         * This method returns the newRange value.
         * @return the value of newRange.
         */
        public int getNewRange() {
            return newRange;
        }


        /**
         * This method returns the oldRange value.
         * @return the value of oldRange.
         */
        public int getOldRange() {
            return oldRange;
        }


        /**
         * This method sets the newRange value.
         * @param newRange the newRange new value.
         */
        public void setNewRange(int newRange) {
            this.newRange = newRange;
        }


        /**
         * This method sets the oldRange value.
         * @param oldRange the oldRange new value.
         */
        public void setOldRange(int oldRange) {
            this.oldRange = oldRange;
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
        private BlueprintBundle blueprintBundle;

        /**
         * Stores new protection range for island.
         */
        private int newRange;

        /**
         * Stores old protection range for island.
         */
        private int oldRange;

        /**
         * Stores old island object
         * @since 1.12.0
         */
        private Island oldIsland;

        /**
         * @since 1.13.0
         */
        private int oldRank;

        /**
         * @since 1.13.0
         */
        private int newRank;

        public IslandEventBuilder island(Island island) {
            this.island = island;
            return this;
        }

        /**
         * @param oldIsland old island object
         * @return IslandEventBuilder
         * @since 1.12.0
         */
        public IslandEventBuilder oldIsland(Island oldIsland) {
            this.oldIsland = oldIsland;
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

        /**
         * @since 1.6.0
         */
        @NonNull
        public IslandEventBuilder blueprintBundle(@NonNull BlueprintBundle blueprintBundle) {
            this.blueprintBundle = blueprintBundle;
            return this;
        }


        /**
         * Allows to set new and old protection range.
         * @param newRange New value of protection range.
         * @param oldRange Old value of protection range.
         * @since 1.11.0
         */
        @NonNull
        public IslandEventBuilder protectionRange(int newRange, int oldRange) {
            this.newRange = newRange;
            this.oldRange = oldRange;
            return this;
        }

        /**
         * @since 1.13.0
         */
        @NonNull
        public IslandEventBuilder rankChange(int oldRank, int newRank){
            this.oldRank = oldRank;
            this.newRank = newRank;
            return this;
        }

        public IslandBaseEvent build() {
            // Call the generic event for developers who just want one event and use the Reason enum
            Bukkit.getPluginManager().callEvent(new IslandEvent(island, player, admin, location, reason));
            // Generate explicit events
            switch (reason) {
            case EXPEL:
                IslandExpelEvent expel = new IslandExpelEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(expel);
                return expel;
            case BAN:
                IslandBanEvent ban = new IslandBanEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(ban);
                return ban;
            case CREATE:
                IslandCreateEvent create = new IslandCreateEvent(island, player, admin, location, blueprintBundle);
                Bukkit.getPluginManager().callEvent(create);
                return create;
            case CREATED:
                IslandCreatedEvent created = new IslandCreatedEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(created);
                return created;
            case DELETE:
                IslandDeleteEvent delete = new IslandDeleteEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(delete);
                return delete;
            case DELETE_CHUNKS:
                IslandDeleteChunksEvent deleteChunks = new IslandDeleteChunksEvent(island, player, admin, location, deletedIslandInfo);
                Bukkit.getPluginManager().callEvent(deleteChunks);
                return deleteChunks;
            case DELETED:
                IslandDeletedEvent deleted = new IslandDeletedEvent(island, player, admin, location, deletedIslandInfo);
                Bukkit.getPluginManager().callEvent(deleted);
                return deleted;
            case ENTER:
                IslandEnterEvent enter = new IslandEnterEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(enter);
                return enter;
            case EXIT:
                IslandExitEvent exit = new IslandExitEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(exit);
                return exit;
            case LOCK:
                IslandLockEvent lock = new IslandLockEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(lock);
                return lock;
            case RESET:
                IslandResetEvent reset = new IslandResetEvent(island, player, admin, location, blueprintBundle, oldIsland);
                Bukkit.getPluginManager().callEvent(reset);
                return reset;
            case RESETTED:
                IslandResettedEvent resetted = new IslandResettedEvent(island, player, admin, location, oldIsland);
                Bukkit.getPluginManager().callEvent(resetted);
                return resetted;
            case UNBAN:
                IslandUnbanEvent unban = new IslandUnbanEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(unban);
                return unban;
            case UNLOCK:
                IslandUnlockEvent unlock = new IslandUnlockEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(unlock);
                return unlock;
            case REGISTERED:
                IslandRegisteredEvent reg = new IslandRegisteredEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(reg);
                return reg;
            case UNREGISTERED:
                IslandUnregisteredEvent unreg = new IslandUnregisteredEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(unreg);
                return unreg;
            case RANGE_CHANGE:
                IslandProtectionRangeChangeEvent
                change = new IslandProtectionRangeChangeEvent(island, player, admin, location, newRange, oldRange);
                Bukkit.getPluginManager().callEvent(change);
                return change;
            case PRECLEAR:
                IslandPreclearEvent preclear = new IslandPreclearEvent(island, player, admin, location, oldIsland);
                Bukkit.getPluginManager().callEvent(preclear);
                return preclear;
            case RESERVED:
                IslandReservedEvent res = new IslandReservedEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(res);
                return res;
            case RANK_CHANGE:
                IslandRankChangeEvent rankChange = new IslandRankChangeEvent(island, player, admin, location, oldRank, newRank);
                Bukkit.getPluginManager().callEvent(rankChange);
                return rankChange;
            default:
                IslandGeneralEvent general = new IslandGeneralEvent(island, player, admin, location);
                Bukkit.getPluginManager().callEvent(general);
                return general;
            }
        }
    }
}
