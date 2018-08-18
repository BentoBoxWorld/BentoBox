package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class AdminDeleteCommand extends ConfirmableCommand {

    public AdminDeleteCommand(CompositeCommand parent) {
        super(parent, "delete");
    }

    @Override
    public void setup() {
        setPermission("admin.delete");
        setParametersHelp("commands.admin.delete.parameters");
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
        return true;
    }

    private void deletePlayer(User user, UUID targetUUID) {
        // Delete player and island
        user.sendMessage("commands.admin.delete.deleted-island", "[xyz]", Util.xyz(getIslands().getIsland(getWorld(), targetUUID).getCenter().toVector()));

        // Get the target's island
        Island oldIsland = getIslands().getIsland(getWorld(), targetUUID);
        if (oldIsland != null) {
            // Check if player is online and on the island
            User target = User.getInstance(targetUUID);
            // Remove them from this island (it still exists and will be deleted later)
            getIslands().removePlayer(getWorld(), targetUUID);
            if (target.isOnline()) {
                // Remove money inventory etc.
                if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
                    target.getPlayer().getEnderChest().clear();
                }
                if (getIWM().isOnLeaveResetInventory(getWorld())) {
                    target.getPlayer().getInventory().clear();
                }
                if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
                    // TODO: needs Vault
                }
            }
            getIslands().deleteIsland(oldIsland, true);
        }
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