package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class AdminInfoCommand extends CompositeCommand {

    public AdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    @Override
    public void setup() {
        setPermission("mod.info");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.info.parameters");
        setDescription("commands.admin.info.description");
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
        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Show info for this player
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        if (island != null) {
            island.showInfo(user);
            if (!getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID).isEmpty()) {
                user.sendMessage("commands.admin.info.islands-in-trash");
            }
            return true;
        } else {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
