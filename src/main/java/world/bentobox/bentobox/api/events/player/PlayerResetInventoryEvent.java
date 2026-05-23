package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's inventory is cleared as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's inventory will not be cleared.
 * This is the only reset event that exposes a {@link HandlerList}, making it the canonical
 * example for the reset-event family.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetInventoryEvent extends PlayerBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     * Returns the list of handlers registered for this event type.
     *
     * @return the handler list
     */
    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    /**
     * Returns the static handler list for this event type.
     * Required by Bukkit's event system so that handlers can be looked up by class.
     *
     * @return the static handler list
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Constructs a new PlayerResetInventoryEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose inventory is being cleared
     */
    public PlayerResetInventoryEvent(World world, Island island, UUID player) {
        // Final variables have to be declared in the constructor
        super(player, island, world);
    }
}