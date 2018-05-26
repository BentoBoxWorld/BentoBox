/**
 *
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

/**
 * @author tastybento
 */
public class IslandGoCommand extends CompositeCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
        new CustomIslandMultiHomeHelp(this);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setDescription("commands.island.go.description");  
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {

        if (!getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return false;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.valueOf(args.get(0));
            int maxHomes = Util.getPermValue(user.getPlayer(), "island.maxhomes", getSettings().getMaxHomes());
            if (homeValue > 1  && homeValue <= maxHomes) {
                getIslands().homeTeleport(getWorld(), user.getPlayer(), homeValue);
                return true;
            }
        }
        getIslands().homeTeleport(getWorld(), user.getPlayer());
        return true;
    }

}
