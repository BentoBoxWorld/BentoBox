package us.tastybento.bskyblock.commands.island;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

/**
 * This is a custom help for the /island go and /island sethome commands. It overrides the default help sub command.
 * The number of homes can change depending on the player's permissions and config.yml settings.
 * @author ben
 *
 */
public class IslandMultiHomeHelp extends CompositeCommand {

    public IslandMultiHomeHelp(CompositeCommand parent) {
        super(parent, "help");        
    }
    
    @Override
    public void setup() {
        this.setOnlyPlayer(true);
        // Inherit parameters from the respective parent class - in this case, only /island go and /island sethome
        this.setParameters(parent.getParameters());
        this.setDescription(parent.getDescription());
        this.setPermission(parent.getPermission());
    }
        
    @Override
    public boolean execute(User user, List<String> args) {
        // This will only be shown if it is for a player
        if (user.isPlayer()) {
            // Get elements
            String params = getParameters().isEmpty() ? "" : user.getTranslation(getParameters()) + " ";
            String desc = getDescription().isEmpty() ? "" : user.getTranslation(getDescription());
            // Player. Check perms
            if (user.hasPermission(getPermission())) {
                int maxHomes = Util.getPermValue(user.getPlayer(), Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                if (maxHomes > 1) {
                    user.sendLegacyMessage(parent.getUsage() + " " + params + desc);
                } else {
                    // No params
                    user.sendLegacyMessage(parent.getUsage() + " " + desc);
                }
                return true;
            } else {
                return true;
            }

        }
        return false;
    }
    
}

