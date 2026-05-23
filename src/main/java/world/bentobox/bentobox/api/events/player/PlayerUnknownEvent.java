package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fallback event fired when a player reset action is requested but no more specific reset event
 * class exists for it.
 * <p>
 * This event is cancellable. If cancelled, the unknown reset action will not be performed.
 * Addon developers should prefer listening to the more specific {@code PlayerReset*} events
 * where possible; this event acts as a catch-all for future or custom reset types.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerUnknownEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerUnknownEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player affected by the unknown reset action
     */
    public PlayerUnknownEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
