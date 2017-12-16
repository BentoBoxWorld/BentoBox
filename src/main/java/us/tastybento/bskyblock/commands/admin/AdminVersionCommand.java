package us.tastybento.bskyblock.commands.admin;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CommandArgument;

public class AdminVersionCommand extends CommandArgument {

    public AdminVersionCommand() {
        super("version");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(Bukkit.getBukkitVersion());
        sender.sendMessage("BSB " + BSkyBlock.getPlugin().getDescription().getVersion());
        return true;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
