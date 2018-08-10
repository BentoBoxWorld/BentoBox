package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class AdminDeleteCommand extends CompositeCommand {

    public AdminDeleteCommand(CompositeCommand parent) {
        super(parent, "delete");
    }
    
    @Override
    public void setup() {
        setPermission("admin.delete");
        setParameters("commands.admin.delete.parameters");
        setDescription("commands.admin.delete.description");
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
        // Team leaders should be kicked before deleting otherwise the whole team will become weird
        if (getIslands().inTeam(getWorld(), targetUUID) && getIslands().getTeamLeader(getWorld(), targetUUID).equals(targetUUID)) {
            user.sendMessage("commands.admin.delete.cannot-delete-team-leader");
            return false;
        }
        // Confirm
        askConfirmation(user, () -> deletePlayer(user, targetUUID));
        return false;
    }
    
    private void deletePlayer(User user, UUID targetUUID) {
        // Delete player and island
        user.sendMessage("commands.admin.delete.deleted-island", "[xyz]", Util.xyz(getIslands().getIsland(getWorld(), targetUUID).getCenter().toVector()));
        getIslands().deleteIsland(getIslands().getIsland(getWorld(), targetUUID), true);
        getIslands().removePlayer(getWorld(), targetUUID);
        getPlayers().clearHomeLocations(getWorld(), targetUUID);        
        user.sendMessage("general.success");
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