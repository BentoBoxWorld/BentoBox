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
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setDescription("commands.island.go.description");
        new CustomIslandMultiHomeHelp(this);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {

        if (getIslands().getIsland(getWorld(), user.getUniqueId()) == null) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return false;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.valueOf(args.get(0));
            int maxHomes = Util.getPermValue(user.getPlayer(), "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (homeValue > 1  && homeValue <= maxHomes) {
                getIslands().homeTeleport(getWorld(), user.getPlayer(), homeValue);
                user.sendMessage("commands.island.go.tip", "[label]", getTopLabel());
                return true;
            }
        }
        getIslands().homeTeleport(getWorld(), user.getPlayer());
        return true;
    }

}
