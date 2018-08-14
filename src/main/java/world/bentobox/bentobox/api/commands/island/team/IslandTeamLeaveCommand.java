package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class IslandTeamLeaveCommand extends ConfirmableCommand {

    public IslandTeamLeaveCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.leave.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("commands.island.team.leave.cannot-leave");
            return false;
        }
        if (!getSettings().isLeaveConfirmation()) {
            leave(user);
            return true;
        } else {
            this.askConfirmation(user, () -> leave(user));
            return false;
        }
    }

    private void leave(User user) {
        UUID leaderUUID = getIslands().getTeamLeader(getWorld(), user.getUniqueId());
        if (leaderUUID != null) {
            User.getInstance(leaderUUID).sendMessage("commands.island.team.leave.left-your-island", TextVariables.NAME, user.getName());
        }
        getIslands().setLeaveTeam(getWorld(), user.getUniqueId());
        // Remove money inventory etc.
        if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
            // TODO: needs Vault
        }
        user.sendMessage("general.success");
    }

}