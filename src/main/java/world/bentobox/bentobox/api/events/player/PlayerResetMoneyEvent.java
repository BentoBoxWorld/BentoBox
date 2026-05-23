package world.bentobox.bentobox.api.events.player;

import org.bukkit.World;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * Fired when a player's in-game money balance is reset to zero (or to the configured starting
 * amount) as part of an island reset.
 * <p>
 * This event is cancellable. If cancelled, the player's balance will not be reset.
 * Requires an economy plugin (e.g. Vault) to be present for the underlying action to take effect.
 * </p>
 *
 * @author tastybento
 * @see PlayerBaseEvent
 */
public class PlayerResetMoneyEvent extends PlayerBaseEvent {

    /**
     * Constructs a new PlayerResetMoneyEvent.
     *
     * @param world  the world in which the reset is occurring
     * @param island the island being reset, or {@code null} if not applicable
     * @param player the UUID of the player whose balance is being reset
     */
    public PlayerResetMoneyEvent(World world, Island island, UUID player) {
        super(player, island, world);
    }
}
