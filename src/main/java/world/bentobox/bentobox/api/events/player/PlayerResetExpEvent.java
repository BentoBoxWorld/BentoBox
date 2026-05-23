package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's experience (XP) is reset as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's experience will not be cleared.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetExpEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerResetExpEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose experience is being reset
     */
    public PlayerResetExpEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
