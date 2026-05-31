package world.bentobox.bentobox.api.events.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player event happens.
 *
 * @author tastybento
 */
public class PlayerEvent {

    public enum Reason {
        INVENTORY_RESET,
        TAMED_REMOVAL,
        ENDERCHEST_RESET,
        MONEY_RESET,
        HEALTH_RESET,
        HUNGER_RESET,
        EXP_RESET,
        UNKNOWN
    }

    public static PlayerEventBuilder builder() {
        return new PlayerEventBuilder();
    }

    public static class PlayerEventBuilder {
        private World world;
        private UUID player;
        private Island island;
        private Reason reason = Reason.UNKNOWN;

        public PlayerEventBuilder world(World world) {
            this.world = world;
            return this;
        }

        public PlayerEventBuilder island(Island island) {
            this.island = island;
            return this;
        }

        /**
         * @param reason for the event
         * @return TeamEventBuilder
         */
        public PlayerEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @param player - the player involved in the event
         * @return PlayerEventBuilder
         */
        public PlayerEventBuilder involvedPlayer(UUID player) {
            this.player = player;
            return this;
        }

        private PlayerBaseEvent getEvent() {
            return switch (reason) {
            case INVENTORY_RESET -> new PlayerResetInventoryEvent(world, island, player);
            case TAMED_REMOVAL -> new PlayerTamedRemovalEvent(world, island, player);
            case ENDERCHEST_RESET -> new PlayerResetEnderChestEvent(world, island, player);
            case MONEY_RESET -> new PlayerResetMoneyEvent(world, island, player);
            case HEALTH_RESET -> new PlayerResetHealthEvent(world, island, player);
            case HUNGER_RESET -> new PlayerResetHungerEvent(world, island, player);
            case EXP_RESET -> new PlayerResetExpEvent(world, island, player);
            case UNKNOWN -> new PlayerUnknownEvent(world, island, player);
            };
        }

        /**
         * Build the event and call it
         * @return event
         */
        public PlayerBaseEvent build() {
            // Generate new event
            PlayerBaseEvent newEvent = getEvent();
            Bukkit.getPluginManager().callEvent(newEvent);
            return newEvent;
        }
    }
}
