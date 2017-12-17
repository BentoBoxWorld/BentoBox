package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.admin.AdminVersionCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Settings.ADMINCOMMAND, "Admin commands", "bsb");
    }

    @Override
    public void setup() {
        this.addSubCommand(new AdminVersionCommand());
    }

    @Override
    public boolean execute(User user, String[] args) {
        return false;
    }
}
