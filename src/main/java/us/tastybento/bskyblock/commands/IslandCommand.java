package us.tastybento.bskyblock.commands;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.config.Settings;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Settings.ISLANDCOMMAND, "Main player command", "is");
    }

    @Override
    public void setup() {

    }
}
