package world.bentobox.bbox.commands.admin.range;

import java.util.List;

import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;

/**
 * @author Poslovitch
 */
public class AdminRangeCommand extends CompositeCommand {

    public AdminRangeCommand(CompositeCommand parent) {
        super (parent, "range");
    }

    @Override
    public void setup() {
        setPermission("admin.range");
        setDescription("commands.admin.range.description");

        new AdminRangeDisplayCommand(this);
        new AdminRangeSetCommand(this);
        new AdminRangeResetCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
