package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class IslandSethomeCommand extends CompositeCommand {

    public IslandSethomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "sethome");
    }

    @Override
    public void setup() {
        setPermission("island.sethome");
        setOnlyPlayer(true);
        setDescription("commands.island.sethome.description");
        new CustomIslandMultiHomeHelp(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Check island
        if (getPlugin().getIslands().getIsland(getWorld(), user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getPlugin().getIslands().userIsOnIsland(getWorld(), user)) {
            user.sendMessage("commands.island.sethome.must-be-on-your-island");
            return false;
        }
        if (args.isEmpty()) {
            // island sethome
            getPlugin().getPlayers().setHomeLocation(playerUUID, user.getLocation());
            user.sendMessage("commands.island.sethome.home-set");
        } else {
            // Dynamic home sizes with permissions
            int maxHomes = Util.getPermValue(user.getPlayer(), "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (maxHomes > 1) {
                // Check the number given is a number
                int number;
                try {
                    number = Integer.valueOf(args.get(0));
                    if (number < 1 || number > maxHomes) {
                        user.sendMessage("commands.island.sethome.num-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
                        return false;
                    } else {
                        getPlugin().getPlayers().setHomeLocation(user, user.getLocation(), number);
                        user.sendMessage("commands.island.sethome.home-set");
                    }
                } catch (Exception e) {
                    user.sendMessage("commands.island.sethome.num-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
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
