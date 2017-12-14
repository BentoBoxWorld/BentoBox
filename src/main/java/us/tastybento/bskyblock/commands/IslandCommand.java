package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.commands.island.IslandAboutCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamCommand;
import us.tastybento.bskyblock.config.Settings;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Settings.ISLANDCOMMAND, "Main player command", "is");
    }

    @Override
    public void setup() {
        this.addSubCommand(new IslandAboutCommand());
        this.addSubCommand(new IslandTeamCommand());
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("You successfully did /is !");
        return true;
    }
}
