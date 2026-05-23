package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's ender chest is cleared as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the ender chest contents will not be cleared.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetEnderChestEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerResetEnderChestEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose ender chest is being cleared
     */
    public PlayerResetEnderChestEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
