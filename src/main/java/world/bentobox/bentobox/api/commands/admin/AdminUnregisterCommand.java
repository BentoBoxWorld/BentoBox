package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class AdminUnregisterCommand extends CompositeCommand {

    public AdminUnregisterCommand(CompositeCommand parent) {
        super(parent, "unregister");
    }
    
    @Override
    public void setup() {
        setPermission("admin.unregister");
        setParametersHelp("commands.admin.unregister.parameters");
        setDescription("commands.admin.unregister.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("commands.admin.unregister.cannot-unregister-team-player");
            return false;
        }
        // Unregister island
        user.sendMessage("commands.admin.unregister.unregistered-island", "[xyz]", Util.xyz(getIslands().getIsland(getWorld(), targetUUID).getCenter().toVector()));
        getIslands().removePlayer(getWorld(), targetUUID);
        getPlayers().clearHomeLocations(getWorld(), targetUUID);
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