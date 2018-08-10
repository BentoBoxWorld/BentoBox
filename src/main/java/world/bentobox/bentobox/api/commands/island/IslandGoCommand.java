package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class IslandGoCommand extends CompositeCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setDescription("commands.island.go.description");
        new CustomIslandMultiHomeHelp(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (getIslands().getIsland(getWorld(), user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.parseInt(args.get(0));
            int maxHomes = Util.getPermValue(user.getPlayer(), "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (homeValue > 1 && homeValue <= maxHomes) {
                getIslands().homeTeleport(getWorld(), user.getPlayer(), homeValue);
                user.sendMessage("commands.island.go.tip", TextVariables.LABEL, getTopLabel());
                return true;
            }
        }
        getIslands().homeTeleport(getWorld(), user.getPlayer());
        return true;
    }

}
