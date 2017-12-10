package us.tastybento.bskyblock.commands.island;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.api.commands.CommandArgument;

import java.util.Set;

public class IslandTeamInviteCommand extends CommandArgument {

    public IslandTeamInviteCommand() {
        super("invite");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("Wants to invite some people, hu?");
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
