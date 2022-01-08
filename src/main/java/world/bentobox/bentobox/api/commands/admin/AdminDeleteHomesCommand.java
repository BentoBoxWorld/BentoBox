package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Deletes all named homes from an island
 * @author tastybento
 *
 */
public class AdminDeleteHomesCommand extends ConfirmableCommand {

    public AdminDeleteHomesCommand(CompositeCommand parent) {
        super(parent, "deletehomes");
    }

    @Override
    public void setup() {
        setPermission("mod.deletehomes");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.deletehomes.parameters");
        setDescription("commands.admin.deletehomes.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get target player
        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Get island
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Confirm
        askConfirmation(user, user.getTranslation("commands.admin.deletehomes.warning"), () -> deleteHomes(user, targetUUID, island));
        return true;
    }

    private boolean deleteHomes(User user, UUID targetUUID, Island island) {
        island.removeHomes();
        user.sendMessage("general.success");
        return true;
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
