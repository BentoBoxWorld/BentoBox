package us.tastybento.bskyblock.commands;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.admin.AdminVersionCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Settings.ADMINCOMMAND, "bsb");
    }

    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "admin.*");
        this.setOnlyPlayer(false);
        this.setDescription("admin.help.description");
        new AdminVersionCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        return this.getSubCommand("help").get().execute(user, args);
    }

}
