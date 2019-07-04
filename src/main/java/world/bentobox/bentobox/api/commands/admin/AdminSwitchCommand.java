package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @since 1.5.0
 * @author tastybento
 */
public class AdminSwitchCommand extends ConfirmableCommand {

    private final String bypassPerm;

    /**
     * Switches bypass on and off
     * @param parent - admin command
     * @since 1.5.0
     */
    public AdminSwitchCommand(CompositeCommand parent) {
        super(parent, "switch");
        bypassPerm = getPermissionPrefix() + "mod.bypassprotect";
    }

    @Override
    public void setup() {
        setPermission("mod.switch");
        setOnlyPlayer(true);
        setParametersHelp("commands.admin.switch.parameters");
        setDescription("commands.admin.switch.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        if (user.isOp()) {
            user.sendMessage("commands.admin.switch.op");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (user.hasPermission(getPermissionPrefix() + "mod.switch")) {
            if (user.hasPermission(bypassPerm)) {
                user.sendMessage("commands.admin.switch.removing");
                // Remove positive perm
                if (user.removePerm(bypassPerm)) {
                    user.sendMessage("general.success");
                }
            } else {
                user.sendMessage("commands.admin.switch.adding");
                // Add positive permission
                user.addPerm(bypassPerm);
                if (user.hasPermission(bypassPerm)) {
                    user.sendMessage("general.success");
                }
            }
        }
        return true;
    }

}
