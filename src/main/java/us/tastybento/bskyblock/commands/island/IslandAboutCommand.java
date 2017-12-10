package us.tastybento.bskyblock.commands.island;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.api.commands.CommandArgument;

import java.util.Set;

public class IslandAboutCommand extends CommandArgument {

    public IslandAboutCommand() {
        super("about", "ab");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("You did /is about successfully!");
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
