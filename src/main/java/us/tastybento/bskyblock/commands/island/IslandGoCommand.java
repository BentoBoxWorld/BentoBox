/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

/**
 * @author ben
 *
 */
public class IslandGoCommand extends CommandArgument {

    public IslandGoCommand() {
        super("go", "home", "h");
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
        if (!player.hasPermission(Settings.PERMPREFIX + "island.home")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }
        if (!getIslands().hasIsland(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-island"));
            return true;
        }
        if (args.length == 1 && NumberUtils.isDigits(args[0])) {
            int homeValue = Integer.valueOf(args[0]);
            int maxHomes = Util.getPermValue(player, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
            if (homeValue > 1  && homeValue <= maxHomes) {
                getIslands().homeTeleport(player, homeValue);
                return true;
            }
        }
        getIslands().homeTeleport(player);
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
