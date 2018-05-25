/**
 *
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.util.Util;

/**
 * @author tastybento
 */
public class IslandGoCommand extends CompositeCommand {

    public IslandGoCommand(IslandCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.home");
        setOnlyPlayer(true);
        setDescription("commands.island.go.description");
        new CustomIslandMultiHomeHelp(this);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        World world = user.getWorld();
        if (!args.isEmpty() && !NumberUtils.isDigits(args.get(0))) {
            // World?
            if (getPlugin().getIWM().isOverWorld(args.get(0))) {
                world = getPlugin().getIWM().getWorld(args.get(0));
            } else {
                // Make a list of worlds
                StringBuilder worlds = new StringBuilder();
                getPlugin().getIWM().getOverWorldNames().forEach(w -> { 
                    worlds.append(w);
                    worlds.append(", ");
                });
                worlds.setLength(worlds.length() - 2);
                user.sendMessage("commands.island.create.pick-world", "[worlds]", worlds.toString());
                return false;
            }
        }
        if (!getIslands().hasIsland(world, user.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return false;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.valueOf(args.get(0));
            int maxHomes = Util.getPermValue(user.getPlayer(), Constants.PERMPREFIX + "island.maxhomes", getSettings().getMaxHomes());
            if (homeValue > 1  && homeValue <= maxHomes) {
                getIslands().homeTeleport(world, user.getPlayer(), homeValue);
                return true;
            }
        }
        getIslands().homeTeleport(world, user.getPlayer());
        return true;
    }

}
