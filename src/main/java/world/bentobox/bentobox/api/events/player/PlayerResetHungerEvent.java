package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's hunger (food level) is reset to the maximum value as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's hunger will not be reset.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetHungerEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerResetHungerEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose hunger is being reset
     */
    public PlayerResetHungerEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
