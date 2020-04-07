package world.bentobox.bentobox.api.commands.admin.resets;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class AdminResetsCommand extends CompositeCommand {

    public AdminResetsCommand(CompositeCommand parent) {
        super(parent, "resets");
    }

    @Override
    public void setup() {
        setPermission("admin.resets");
        setDescription("commands.admin.resets.description");

        new AdminResetsSetCommand(this);
        new AdminResetsResetCommand(this);
        new AdminResetsAddCommand(this);
        new AdminResetsRemoveCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
