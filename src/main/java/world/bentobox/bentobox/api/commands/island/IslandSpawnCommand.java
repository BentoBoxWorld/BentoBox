package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Player command to teleport oneself to the world's spawn.
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

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if ((getIWM().inWorld(user.getWorld()) && Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()))
                && user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }

        this.delayCommand(user, () -> getIslands().spawnTeleport(getWorld(), user.getPlayer()));
        return true;
    }
}
