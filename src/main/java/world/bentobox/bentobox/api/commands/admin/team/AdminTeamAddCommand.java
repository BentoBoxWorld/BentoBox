package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class AdminTeamAddCommand extends CompositeCommand {

    public AdminTeamAddCommand(CompositeCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setPermission("admin.team");
        setParametersHelp("commands.admin.team.add.parameters");
        setDescription("commands.admin.team.add.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }
        // Get leader and target
        UUID leaderUUID = getPlayers().getUUID(args.get(0));
        if (leaderUUID == null) {
            user.sendMessage("general.errors.unknown-player-name", TextVariables.NAME, args.get(0));
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.get(1));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player-name", TextVariables.NAME, args.get(1));
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), leaderUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(getWorld(), leaderUUID) && !getIslands().getTeamLeader(getWorld(), leaderUUID).equals(leaderUUID)) {
            user.sendMessage("commands.admin.team.add.name-not-leader", TextVariables.NAME, args.get(0));
            getIslands().getIsland(getWorld(), leaderUUID).showMembers(getPlugin(), user, getWorld());
            return false;
        }
        if (getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }
        if (getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("commands.admin.team.add.name-has-island", TextVariables.NAME, args.get(1));
            return false;
        }
        // Success
        User target = User.getInstance(targetUUID);
        User leader = User.getInstance(leaderUUID);
        leader.sendMessage("commands.island.team.invite.accept.name-joined-your-island", TextVariables.NAME, getPlugin().getPlayers().getName(targetUUID));
        target.sendMessage("commands.island.team.invite.accept.you-joined-island", TextVariables.LABEL, getTopLabel());
        getIslands().getIsland(getWorld(), leaderUUID).addMember(targetUUID);
        user.sendMessage("general.success");
        return true;

    }


}
