package us.tastybento.bskyblock.commands;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.island.IslandAboutCommand;
import us.tastybento.bskyblock.commands.island.IslandCreateCommand;
import us.tastybento.bskyblock.commands.island.IslandGoCommand;
import us.tastybento.bskyblock.commands.island.IslandResetCommand;
import us.tastybento.bskyblock.commands.island.IslandResetnameCommand;
import us.tastybento.bskyblock.commands.island.IslandSethomeCommand;
import us.tastybento.bskyblock.commands.island.IslandSetnameCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamCommand;
import us.tastybento.bskyblock.config.Settings;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Settings.ISLANDCOMMAND, "Main player command", "is");
    }

    @Override
    public void setup() {
        this.addSubCommand(new IslandAboutCommand());
        this.addSubCommand(new IslandCreateCommand());
        this.addSubCommand(new IslandGoCommand());
        this.addSubCommand(new IslandResetCommand());
        this.addSubCommand(new IslandResetnameCommand());
        this.addSubCommand(new IslandSethomeCommand());
        this.addSubCommand(new IslandSetnameCommand());
        this.addSubCommand(new IslandTeamCommand());
    }

    @Override
    public boolean execute(User user, String[] args) {
        user.sendLegacyMessage("You successfully did /is !");
        return true;
    }
}
