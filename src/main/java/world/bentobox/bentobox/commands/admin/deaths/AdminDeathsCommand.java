package world.bentobox.bentobox.commands.admin.deaths;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

import java.util.List;

/**
 * @author Poslovitch
 */
public class AdminDeathsCommand extends CompositeCommand {

    public AdminDeathsCommand(CompositeCommand parent) {
        super(parent, "deaths");
    }

    @Override
    public void setup() {
        setPermission("admin.deaths");
        setDescription("commands.admin.deaths.description");

        new AdminDeathsResetCommand(this);
        new AdminDeathsSetCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
