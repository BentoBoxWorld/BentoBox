package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * This is a custom help for the /island go and /island sethome commands. It overrides the default help sub command.
 * The number of homes can change depending on the player's permissions and config.yml settings.
 * This is an example of a custom help as much as anything.
 *
 * @author tastybento
 *
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
    public boolean execute(User user, String label, List<String> args) {
        // This will only be shown if it is for a player
        if (user.isPlayer()) {
            // Get elements
            String usage = parent.getUsage().isEmpty() ? "" : user.getTranslation(parent.getUsage());
            String params = "";
            String desc = getDescription().isEmpty() ? "" : user.getTranslation(getDescription());

            showPrettyHelp(user, usage, params, desc);
            return true;
        }
        return false;
    }

    private void showPrettyHelp(User user, String usage, String params, String desc) {
        // Player. Check perms
        if (user.hasPermission(getPermission())) {
            int maxHomes = Util.getPermValue(user.getPlayer(), getPermissionPrefix() + "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (maxHomes > 1) {
                params = getParameters().isEmpty() ? "" : user.getTranslation(getParameters());
            }
            user.sendMessage("commands.help.syntax", "[usage]", usage, "[parameters]", params, "[description]", desc);
        }   
    }

}

