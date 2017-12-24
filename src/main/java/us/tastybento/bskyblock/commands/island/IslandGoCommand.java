/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

/**
 * @author ben
 *
 */
public class IslandGoCommand extends CompositeCommand {

    public IslandGoCommand(IslandCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
        this.setPermission(Settings.PERMPREFIX + "island.home");
        this.setOnlyPlayer(true);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        if (!getIslands().hasIsland(user.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return true;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.valueOf(args.get(0));
            int maxHomes = Util.getPermValue(user.getPlayer(), Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
            if (homeValue > 1  && homeValue <= maxHomes) {
                getIslands().homeTeleport(user.getPlayer(), homeValue);
                return true;
            }
        }
        getIslands().homeTeleport(user.getPlayer());
        return true;
    }

}
