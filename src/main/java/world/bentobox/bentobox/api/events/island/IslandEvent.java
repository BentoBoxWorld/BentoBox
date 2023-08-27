package world.bentobox.bentobox.api.events.island;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
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
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

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
         * Fired when a player tries to create a new island. If canceled will
         * proceed no further.
         * @since 1.15.1
         */
        PRECREATE,
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
         * Called when a player goes to their new island for the first time
         * @since 1.16.1
         */
        NEW_ISLAND,
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
        RANK_CHANGE,
        /**
         * Event that will fire when an island is named or renamed
         * @since 1.24.0
         */
        NAME,
        /**
         * Event that will fire when the info command is executed. Allows addons to add to it
         * @since 1.24.0
         */
        INFO
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
        private IslandDeletion deletedIslandInfo;
        private BlueprintBundle blueprintBundle;
        private Event rawEvent;

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
         * Store old islands
         * @since 2.0.0
         */
        private Set<Island> oldIslands;

        /**
         * @since 1.13.0
         */
        private int oldRank;

        /**
         * @since 1.13.0
         */
        private int newRank;
        /**
         * @since 1.24.0 Previous name of island
         */
        private String previousName;
        /**
         * @since 1.24.0 GameMode addon causing this event
         */
        private Addon addon;

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

        public IslandEventBuilder rawEvent(Event event) {
            rawEvent = event;
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
         * @param oldIslands2 set of old islands
         * @since 2.0.0
         */
        public IslandEventBuilder oldIslands(Set<Island> oldIslands2) {
            this.oldIslands = new HashSet<>(oldIslands2);
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

        /**
         * Sets the previous name of the island
         * @param previousName previous name. May be null.
         * @since 1.24.0
         */
        public IslandEventBuilder previousName(@Nullable String previousName) {
            this.previousName = previousName;
            return this;
        }

        /**
         * Addon that triggered this event, e.g. BSkyBlock
         * @param addon Addon.
         * @since 1.24.0
         */
        public IslandEventBuilder addon(Addon addon) {
            this.addon = addon;
            return this;
        }

        private IslandBaseEvent getEvent() {
            return switch (reason) {
            case EXPEL -> new IslandExpelEvent(island, player, admin, location);
            case BAN -> new IslandBanEvent(island, player, admin, location);
            case PRECREATE -> new IslandPreCreateEvent(player);
            case CREATE -> new IslandCreateEvent(island, player, admin, location, blueprintBundle);
            case CREATED -> new IslandCreatedEvent(island, player, admin, location);
            case DELETE -> new IslandDeleteEvent(island, player, admin, location);
            case DELETE_CHUNKS -> new IslandDeleteChunksEvent(island, player, admin, location, deletedIslandInfo);
            case DELETED -> new IslandDeletedEvent(island, player, admin, location, deletedIslandInfo);
            case ENTER -> new IslandEnterEvent(island, player, admin, location, oldIsland, rawEvent);
            case EXIT -> new IslandExitEvent(island, player, admin, location, oldIsland, rawEvent);
            case LOCK -> new IslandLockEvent(island, player, admin, location);
            case RESET -> new IslandResetEvent(island, player, admin, location, blueprintBundle, oldIslands);
            case RESETTED -> new IslandResettedEvent(island, player, admin, location, oldIsland);
            case UNBAN -> new IslandUnbanEvent(island, player, admin, location);
            case UNLOCK -> new IslandUnlockEvent(island, player, admin, location);
            case REGISTERED -> new IslandRegisteredEvent(island, player, admin, location);
            case UNREGISTERED -> new IslandUnregisteredEvent(island, player, admin, location);
            case RANGE_CHANGE -> new IslandProtectionRangeChangeEvent(island, player, admin, location, newRange, oldRange);
            case PRECLEAR -> new IslandPreclearEvent(island, player, admin, location, oldIsland);
            case RESERVED -> new IslandReservedEvent(island, player, admin, location);
            case RANK_CHANGE -> new IslandRankChangeEvent(island, player, admin, location, oldRank, newRank);
            case NEW_ISLAND -> new IslandNewIslandEvent(island, player, admin, location);
            case NAME -> new IslandNameEvent(island, player, admin, location, previousName);
            case INFO -> new IslandInfoEvent(island, player, admin, location, addon);
            default -> new IslandGeneralEvent(island, player, admin, location);
            };
        }

        /**
         * Builds and fires the deprecated and new IslandEvent.
         * @return deprecated event. To obtain the new event use {@link IslandBaseEvent#getNewEvent()}
         */
        public IslandBaseEvent build() {
            // Call the generic event for developers who just want one event and use the Reason enum
            Bukkit.getPluginManager().callEvent(new IslandEvent(island, player, admin, location, reason));
            // Generate new event
            IslandBaseEvent newEvent = getEvent();
            Bukkit.getPluginManager().callEvent(newEvent);
            return newEvent;
        }

    }
}
