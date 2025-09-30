package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles the island spawn command (/island spawn).
 * <p>
 * This command teleports players to the world spawn location with
 * configurable delay and safety checks. Extends {@link DelayedTeleportCommand}
 * to provide teleport delay functionality.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable teleport delay</li>
 *   <li>Fall protection (prevents teleporting while falling)</li>
 *   <li>Player-only command</li>
 *   <li>Permission-based access</li>
 * </ul>
 * <p>
 * Permission: {@code island.spawn}
 *
 * @author Poslovitch
 * @since 1.1
 */
public class IslandSpawnCommand extends DelayedTeleportCommand {

    public IslandSpawnCommand(CompositeCommand parent) {
        super(parent, "spawn");
    }

    @Override
    public void setup() {
        setPermission("island.spawn");
        setOnlyPlayer(true);
        setDescription("commands.island.spawn.description");
    }

    /**
     * Handles the spawn teleport request.
     * <p>
     * Process:
     * <ul>
     *   <li>Checks if player is falling (if flag is set)</li>
     *   <li>Initiates delayed teleport to spawn</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check if player is falling and teleport prevention flag is set
        if ((getIWM().inWorld(user.getWorld()) && 
             Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld())) &&
            user.getPlayer().getFallDistance() > 0) {
            // Send hint message about prevented teleport
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }

        // Initiate delayed teleport to spawn
        this.delayCommand(user, () -> getIslands().spawnTeleport(getWorld(), user.getPlayer()));
        return true;
    }
}
