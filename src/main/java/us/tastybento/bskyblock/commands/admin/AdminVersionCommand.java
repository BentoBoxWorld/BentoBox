package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

public class AdminVersionCommand extends CompositeCommand {

    public AdminVersionCommand(CompositeCommand adminCommand) {
        super(adminCommand, "version");
    }

    @Override
    public void setup() {
        // Permission
        this.setPermission(Constants.PERMPREFIX + "admin.version");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        return false;
    }

}
