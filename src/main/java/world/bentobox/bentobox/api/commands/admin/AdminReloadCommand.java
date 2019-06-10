package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Generic reload command for addons to use that will just call the onReload() method
 * after checking permissions
 * @author tastybento
 */
public class AdminReloadCommand extends CompositeCommand {

    public AdminReloadCommand(CompositeCommand adminCommand) {
        super(adminCommand, "reload", "rl");
    }

    @Override
    public void setup() {
        // Permission
        setPermission("admin.reload");
        setDescription("commands.admin.reload.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        getAddon().onReload();
        return true;
    }

}
