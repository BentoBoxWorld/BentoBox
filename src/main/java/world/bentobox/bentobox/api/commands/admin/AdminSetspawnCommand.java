package world.bentobox.bentobox.api.commands.admin;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.Optional;

/**
 * Admin command (only player) to set an island as the world's spawn.
 * @author Poslovitch
 * @since 1.1
 */
public class AdminSetspawnCommand extends ConfirmableCommand {

    public AdminSetspawnCommand(CompositeCommand parent) {
        super(parent, "setspawn");
    }

    @Override
    public void setup() {
        setPermission("admin.setspawn");
        setOnlyPlayer(true);
        setDescription("commands.admin.setspawn.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Optional<Island> island = getIslands().getIslandAt(user.getLocation());

        if (island.isPresent()) {
            // Check if the island is already a spawn
            if (island.map(Island::isSpawn).orElse(false)) {
                user.sendMessage("commands.admin.setspawn.already-spawn");
                return false;
            }

            // Everything's fine, we can set the island as spawn :)
            askConfirmation(user, "commands.admin.setspawn.confirmation", () -> {
                getIslands().setSpawn(island.get());
                user.sendMessage("general.success");
            });
            return true;
        } else {
            user.sendMessage("commands.admin.setspawn.no-island-here");
            return false;
        }
    }
}
