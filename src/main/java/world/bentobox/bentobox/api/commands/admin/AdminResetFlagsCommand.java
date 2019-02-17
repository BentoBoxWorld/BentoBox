package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Admin command to reset all islands in a world to the default flag setting in the game mode config.yml
 * @author tastybento
 * @since 1.3.0
 */
public class AdminResetFlagsCommand extends ConfirmableCommand {

    public AdminResetFlagsCommand(CompositeCommand parent) {
        super(parent, "resetflags");
    }

    @Override
    public void setup() {
        setPermission("admin.resetflags");
        setOnlyPlayer(false);
        setDescription("commands.admin.resetflags.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Everything's fine, we can set the island as spawn :)
        askConfirmation(user, () -> {
            getIslands().resetAllFlags(getWorld());
            user.sendMessage("general.success");
        });
        return true;
    }

}
