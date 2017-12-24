package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class AdminVersionCommand extends CompositeCommand {

    public AdminVersionCommand(CompositeCommand adminCommand) {
        super(adminCommand, "version");
        // Permission
        this.setPermission(Settings.PERMPREFIX + "admin.version");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        return false;
    }

}
