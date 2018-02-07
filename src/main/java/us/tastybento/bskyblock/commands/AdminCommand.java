package us.tastybento.bskyblock.commands;

import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.admin.AdminReloadCommand;
import us.tastybento.bskyblock.commands.admin.AdminTeleportCommand;
import us.tastybento.bskyblock.commands.admin.AdminVersionCommand;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Constants.ADMINCOMMAND, "bsb");
    }

    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "admin.*");
        this.setOnlyPlayer(false);
        this.setDescription("admin.help.description");
        new AdminVersionCommand(this);
        new AdminReloadCommand(this);
        new AdminTeleportCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // By default run the attached help command, if it exists (it should)
        showHelp(this, user, args);
        return false;
    }

}
