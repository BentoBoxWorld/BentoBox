/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
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
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        Player player = (Player)user;
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission(Settings.PERMPREFIX + "island.name")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return true;
        }

        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage(ChatColor.RED + "general.errors.not-leader");
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
            user.sendMessage("general.errors.too-short", "[length]",  String.valueOf(Settings.nameMinLength));
            return true;
        }
        if (name.length() > Settings.nameMaxLength) {
            user.sendMessage("general.errors.too-long", "[length]", String.valueOf(Settings.nameMaxLength));
            return true;
        }

        // Set the name
        if (!player.hasPermission(Settings.PERMPREFIX + "island.name.format"))
            getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
        else getIslands().getIsland(playerUUID).setName(name);

        user.sendMessage("general.success");
        return true;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

}
