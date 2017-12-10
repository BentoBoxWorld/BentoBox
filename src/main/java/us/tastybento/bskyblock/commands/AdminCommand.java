package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Settings.ADMINCOMMAND, "Admin commands");
    }

    @Override
    public void setup() {

    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return false;
    }
}
