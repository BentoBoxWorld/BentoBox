/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
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
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        Player player = (Player)user;
        if (!player.hasPermission(Settings.PERMPREFIX + "island.home")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return true;
        }
        if (!getIslands().hasIsland(player.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
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
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

}
