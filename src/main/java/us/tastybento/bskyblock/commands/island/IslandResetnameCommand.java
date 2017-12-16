/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author ben
 *
 */
public class IslandResetnameCommand extends CommandArgument {

    public IslandResetnameCommand() {
        super("setname");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission(Settings.PERMPREFIX + "island.name")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }

        if (!getIslands().hasIsland(playerUUID)) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-island"));
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.not-leader"));
            return true;
        }
        // Explain command
        if (args.length == 1) {
            //TODO Util.sendMessage(player, getHelpMessage(player, label, args[0], usage(sender, label)));
            return true;
        }

        // Naming the island
        String name = args[1];
        for (int i = 2; i < args.length; i++) name += " " + args[i];

        // Check if the name isn't too short or too long
        if (name.length() < Settings.nameMinLength) {
            sender.sendMessage(getLocale(sender).get("general.errors.too-short").replace("[length]", String.valueOf(Settings.nameMinLength)));
            return true;
        }
        if (name.length() > Settings.nameMaxLength) {
            sender.sendMessage(getLocale(sender).get("general.errors.too-long").replace("[length]", String.valueOf(Settings.nameMaxLength)));
            return true;
        }

        // Set the name
        if (!player.hasPermission(Settings.PERMPREFIX + "island.name.format"))
            getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
        else getIslands().getIsland(playerUUID).setName(name);

        sender.sendMessage(getLocale(sender).get("general.success"));
        return true;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

}
