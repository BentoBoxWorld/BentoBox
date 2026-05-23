package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's tamed entities (wolves, cats, horses, etc.) are removed as part of an
 * island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's tamed entities will not be removed.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerTamedRemovalEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerTamedRemovalEvent.
     *
     * @param world  the world in which the removal is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose tamed entities are being removed
     */
    public PlayerTamedRemovalEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
