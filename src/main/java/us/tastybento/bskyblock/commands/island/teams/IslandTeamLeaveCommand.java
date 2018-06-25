package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.user.User;

public class IslandTeamLeaveCommand extends CompositeCommand {

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
    public boolean execute(User user, List<String> args) {
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
        } else {
            this.askConfirmation(user, () -> leave(user));
        }
        return true;
    }

    private void leave(User user) {
        UUID leaderUUID = getIslands().getTeamLeader(getWorld(), user.getUniqueId());
        if (leaderUUID != null) {
            User.getInstance(leaderUUID).sendMessage("commands.island.team.leave.left-your-island", TextVariables.NAME, user.getName());
        }
        getIslands().removePlayer(getWorld(), user.getUniqueId());
        user.sendMessage("general.success");
    }

}