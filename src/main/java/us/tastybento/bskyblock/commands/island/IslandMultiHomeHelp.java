package us.tastybento.bskyblock.commands.island;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

/**
 * This is a customer help for the /island go and /island sethome commands. It overrides the default help sub command.
 * The number of homes can change depending on the player's permissions and config.yml settings.
 * @author ben
 *
 */
public class IslandMultiHomeHelp extends CompositeCommand {

    private CompositeCommand parent;

    public IslandMultiHomeHelp(CompositeCommand parent) {
        super(parent, "help");
        this.parent = parent;
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (user.isPlayer()) {
            user.sendLegacyMessage("DEBUG: Custom help");
            // Player. Check perms
            if (user.hasPermission(parent.getPermission())) {
                int maxHomes = Util.getPermValue(user.getPlayer(), Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
                if (maxHomes > 1) {
                    user.sendMessage((parent.getUsage().isEmpty() ? "" : parent.getUsage() + " ") + parent.getDescription());
                } else {
                    user.sendMessage(parent.getDescription());
                }
                return true;
            } else {
                user.sendMessage("errors.no-permission");
                return true;
            }

        }
        return false;
    }
    
}

