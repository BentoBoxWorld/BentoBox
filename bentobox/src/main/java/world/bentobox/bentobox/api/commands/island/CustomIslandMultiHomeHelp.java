package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * This is a custom help for the /island go and /island sethome commands. It overrides the default help sub command.
 * The number of homes can change depending on the player's permissions and config.yml settings.
 * This is an example of a custom help as much as anything.
 *
 * @author tastybento
 */
public class CustomIslandMultiHomeHelp extends CompositeCommand {

    public CustomIslandMultiHomeHelp(CompositeCommand parent) {
        super(parent, "help");
    }

    @Override
    public void setup() {
        setOnlyPlayer(true);
        // Inherit parameters from the respective parent class - in this case, only /island go and /island sethome
        setParametersHelp(parent.getParameters());
        setDescription(parent.getDescription());
        inheritPermission();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return user.isPlayer() && user.hasPermission(getPermission());
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get elements
        String usage = parent.getUsage().isEmpty() ? "" : user.getTranslation(parent.getUsage());
        int maxHomes = user.getPermissionValue(getPermissionPrefix() + "island.maxhomes", getIWM().getMaxHomes(getWorld()));
        String params = maxHomes > 1 ? user.getTranslation(getParameters()) : "";
        String desc = getDescription().isEmpty() ? "" : user.getTranslation(getDescription());
        user.sendMessage("commands.help.syntax", "[usage]", usage, "[parameters]", params, "[description]", desc);
        return true;
    }

}

