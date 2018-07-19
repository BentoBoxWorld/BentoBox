package us.tastybento.bskyblock.commands.admin.range;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

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
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
