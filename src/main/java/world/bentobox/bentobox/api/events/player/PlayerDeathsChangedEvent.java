package world.bentobox.bentobox.api.events.player;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.BentoBoxEvent;

/**
 * Fired after an admin changes a player's death count with one of the
 * {@code deaths} admin commands. It is not fired for deaths recorded naturally,
 * or for the automatic resets done when a player gets a new island or joins a team.
 * <p>
 * Addons that keep their own death counts (e.g. Level) can listen for this event
 * so that the admin deaths commands also apply to them.
 *
 * @author tastybento
 * @since 3.23.1
 */
public class PlayerDeathsChangedEvent extends BentoBoxEvent {

    /**
     * The admin command that changed the deaths.
     */
    public enum Action {
        /** Deaths were set to {@link PlayerDeathsChangedEvent#getAmount()} */
        SET,
        /** {@link PlayerDeathsChangedEvent#getAmount()} deaths were added */
        ADD,
        /** {@link PlayerDeathsChangedEvent#getAmount()} deaths were removed */
        REMOVE,
        /** Deaths were reset to zero */
        RESET
    }

    private static final HandlerList handlers = new HandlerList();

    private final World world;
    private final UUID playerUUID;
    private final Action action;
    private final int amount;
    private final int oldDeaths;
    private final int newDeaths;

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @param world      the game mode world whose death count changed
     * @param playerUUID the player whose deaths changed
     * @param action     the admin action that changed the deaths
     * @param amount     the amount the admin requested; for {@link Action#RESET} this is zero
     * @param oldDeaths  the player's death count in this world before the change
     * @param newDeaths  the player's death count in this world after the change
     */
    public PlayerDeathsChangedEvent(World world, UUID playerUUID, Action action, int amount, int oldDeaths,
            int newDeaths) {
        this.world = world;
        this.playerUUID = playerUUID;
        this.action = action;
        this.amount = amount;
        this.oldDeaths = oldDeaths;
        this.newDeaths = newDeaths;
    }

    /**
     * @return the game mode world whose death count changed
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the player whose deaths changed
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * @return the admin action that changed the deaths
     */
    public Action getAction() {
        return action;
    }

    /**
     * The amount requested by the admin. For {@link Action#REMOVE} this may be more than
     * the actual decrease, because BentoBox does not let the count go below zero.
     * @return the amount requested; zero for {@link Action#RESET}
     */
    public int getAmount() {
        return amount;
    }

    /**
     * @return the player's death count in this world before the change
     */
    public int getOldDeaths() {
        return oldDeaths;
    }

    /**
     * @return the player's death count in this world after the change
     */
    public int getNewDeaths() {
        return newDeaths;
    }
}
