package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class IslandInfoCommand extends CompositeCommand {

    public IslandInfoCommand(CompositeCommand parent) {
        super(parent, "info", "who");
    }

    @Override
    public void setup() {
        setPermission("island.info");
        setOnlyPlayer(false);
        setParametersHelp("commands.island.info.parameters");
        setDescription("commands.island.info.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1 || (args.isEmpty() && !user.isPlayer())) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // If there are no args, then the player wants info on the island at this location
        if (args.isEmpty()) {
            if (!getIslands().getIslandAt(user.getLocation()).map(i -> i.showInfo(user)).orElse(false)) {
                user.sendMessage("commands.admin.info.no-island");
                return false;
            }
            return true;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Show info for this player
        getIslands().getIsland(getWorld(), targetUUID).showInfo(user);
        return true;
    }
}
