package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Displays the current status and progress of the purge.
 * @since 1.13.0
 * @author Poslovitch
 */
public class AdminPurgeStatusCommand extends CompositeCommand {

    public AdminPurgeStatusCommand(AdminPurgeCommand parent) {
        super(parent, "status");
    }

    @Override
    public void setup() {
        inheritPermission();
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.status.parameters");
        setDescription("commands.admin.purge.status.description");
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
            int purged = parentCommand.getPurgedIslandsCount();
            int purgeable = parentCommand.getPurgeableIslandsCount();
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            user.sendMessage("commands.admin.purge.status.status",
                    "[purged]", String.valueOf(purged),
                    "[purgeable]", String.valueOf(purgeable),
                    "[percentage]", String.format("%.1f", (((float) purged)/purgeable) * 100));
        } else {
            user.sendMessage("commands.admin.purge.no-purge-in-progress");
        }
        return true;
    }
}
