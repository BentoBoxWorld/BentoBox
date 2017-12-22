package us.tastybento.bskyblock.commands;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.admin.AdminVersionCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Settings.ADMINCOMMAND, "Admin commands", "bsb");
        this.setPermission(Settings.PERMPREFIX + "admin.*");
        this.setOnlyPlayer(false);
    }

    @Override
    public void setup() {
        new AdminVersionCommand(this);
    }

    @Override
    public boolean execute(User user, String[] args) {
        return false;
    }
}
