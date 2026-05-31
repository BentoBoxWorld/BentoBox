package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's health is reset to the maximum value as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's health will not be reset.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetHealthEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerResetHealthEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose health is being reset
     */
    public PlayerResetHealthEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
