package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when island protection range is changed.
 * @since 1.11.0
 */
public class IslandProtectionRangeChangeEvent extends IslandBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

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
    IslandProtectionRangeChangeEvent(Island island, UUID player, boolean admin, Location location, int newRange, int oldRange) {
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