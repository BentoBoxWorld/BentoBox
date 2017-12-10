package us.tastybento.bskyblock.commands.island;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.api.commands.CommandArgument;

import java.util.Set;

public class IslandTeamCommand extends CommandArgument {

    public IslandTeamCommand() {
        super("team");
        this.addSubCommand(new IslandTeamInviteCommand());
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("Hey, you've got a pretty team there :D");
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
