package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

public class IslandSethomeCommand extends CompositeCommand {

    public IslandSethomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "sethome");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.sethome");
        setOnlyPlayer(true);
        setDescription("commands.island.sethome.description");
        new CustomIslandMultiHomeHelp(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Check island
        if (getPlugin().getIslands().getIsland(user.getWorld(), user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getPlugin().getIslands().userIsOnIsland(user.getWorld(), user)) {
            user.sendMessage("commands.island.sethome.must-be-on-your-island");
            return false;
        }
        if (args.isEmpty()) {
            // island sethome
            getPlugin().getPlayers().setHomeLocation(playerUUID, user.getLocation());
            user.sendMessage("commands.island.sethome.home-set");
        } else {
            // Dynamic home sizes with permissions
            int maxHomes = Util.getPermValue(user.getPlayer(), Constants.PERMPREFIX + "island.maxhomes", getSettings().getMaxHomes());
            if (maxHomes > 1) {
                // Check the number given is a number
                int number = 0;
                try {
                    number = Integer.valueOf(args.get(0));
                    if (number < 1 || number > maxHomes) {
                        user.sendMessage("commands.island.sethome.num-homes", "[max]", String.valueOf(maxHomes));
                        return false;
                    } else {
                        getPlugin().getPlayers().setHomeLocation(user, user.getLocation(), number);
                        user.sendMessage("commands.island.sethome.home-set");
                    }
                } catch (Exception e) {
                    user.sendMessage("commands.island.sethome.num-homes", "[max]", String.valueOf(maxHomes));
                    return false;
                }
            } else {
                user.sendMessage("general.errors.no-permission");
                return false;
            }
        }
        return true;
    }

}
