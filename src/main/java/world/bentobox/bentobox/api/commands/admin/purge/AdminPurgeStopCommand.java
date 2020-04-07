package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class AdminPurgeStopCommand extends CompositeCommand {

    public AdminPurgeStopCommand(CompositeCommand parent) {
        super(parent, "stop", "cancel");
    }

    @Override
    public void setup() {
        inheritPermission();
        setOnlyPlayer(false);
        setDescription("commands.admin.purge.stop.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        AdminPurgeCommand parentCommand = ((AdminPurgeCommand)getParent());
        if (parentCommand.isInPurge()) {
            user.sendMessage("commands.admin.purge.stop.stopping");
            parentCommand.stop();
            return true;
        } else {
            user.sendMessage("commands.admin.purge.no-purge-in-progress");
            return false;
        }
    }
}
