package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class IslandSethomeCommand extends ConfirmableCommand {

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
            setHome(user, 1);
            return true;
        } else {
            // Dynamic home sizes with permissions
            int maxHomes = user.getPermissionValue(getPermissionPrefix() + "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (maxHomes > 1) {
                // Check the number given is a number
                int number;
                try {
                    number = Integer.valueOf(args.get(0));
                    if (number < 1 || number > maxHomes) {
                        user.sendMessage("commands.island.sethome.num-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
                        return false;
                    } else {
                        setHome(user, number);
                        return true;
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
    }

    private void setHome(User user, int number) {
        // Check if the player is in the Nether
        if (getIWM().isNether(user.getLocation().getWorld())) {
            // Check if he is (not) allowed to set his home here
            if (!getIWM().getWorldSettings(user.getLocation().getWorld()).isAllowSetHomeInNether()) {
                user.sendMessage("commands.island.sethome.nether.not-allowed");
                return;
            }

            // Check if a confirmation is required
            if (getIWM().getWorldSettings(user.getLocation().getWorld()).isRequireConfirmationToSetHomeInNether()) {
                askConfirmation(user, "commands.island.sethome.nether.confirmation", () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else if (getIWM().isEnd(user.getLocation().getWorld())) { // Check if the player is in the End
            // Check if he is (not) allowed to set his home here
            if (!getIWM().getWorldSettings(user.getLocation().getWorld()).isAllowSetHomeInTheEnd()) {
                user.sendMessage("commands.island.sethome.the-end.not-allowed");
                return;
            }

            // Check if a confirmation is required
            if (getIWM().getWorldSettings(user.getLocation().getWorld()).isRequireConfirmationToSetHomeInTheEnd()) {
                askConfirmation(user, user.getTranslation("commands.island.sethome.the-end.confirmation"), () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else { // The player is in the Overworld, no need to run a check
            doSetHome(user, number);
        }
    }

    private void doSetHome(User user, int number) {
        // Define a runnable as we will be using it often in the code below.
        getPlugin().getPlayers().setHomeLocation(user, user.getLocation(), number);
        user.sendMessage("commands.island.sethome.home-set");

    }
}
