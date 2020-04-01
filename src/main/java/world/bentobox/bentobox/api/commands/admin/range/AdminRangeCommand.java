package world.bentobox.bentobox.api.commands.admin.range;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class AdminRangeCommand extends CompositeCommand {

    public AdminRangeCommand(CompositeCommand parent) {
        super (parent, "range");
        setPermission("admin.range");
        setDescription("commands.admin.range.description");

        new AdminRangeDisplayCommand(this);
        new AdminRangeSetCommand(this);
        new AdminRangeResetCommand(this);
        new AdminRangeAddCommand(this);
        new AdminRangeRemoveCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
