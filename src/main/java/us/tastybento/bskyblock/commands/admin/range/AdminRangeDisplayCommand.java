package us.tastybento.bskyblock.commands.admin.range;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

import java.util.List;

/**
 * @author Poslovitch
 */
public class AdminRangeDisplayCommand extends CompositeCommand {

    public AdminRangeDisplayCommand(CompositeCommand parent) {
        super(parent, "display", "show", "hide");
    }

    @Override
    public void setup() {
        setOnlyPlayer(true);
        setPermission("admin.range.display");
        setParameters("commands.admin.range.display.parameters");
        setDescription("commands.admin.range.display.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Just to try out the things
        user.sendRawMessage(label);
        for (String a: args) {
            user.sendRawMessage(a);
        }
        return false;
    }
}
