package us.tastybento.bskyblock.commands;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.config.Settings;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Settings.ADMINCOMMAND, "Admin commands");
    }

    @Override
    public void setup() {

    }
}
