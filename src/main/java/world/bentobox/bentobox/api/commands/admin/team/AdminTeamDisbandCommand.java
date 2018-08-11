package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class AdminTeamDisbandCommand extends CompositeCommand {

    public AdminTeamDisbandCommand(CompositeCommand parent) {
        super(parent, "disband");
    }
    
    @Override
    public void setup() {
        setPermission("admin.team");
        setParametersHelp("commands.admin.team.disband.parameters");
        setDescription("commands.admin.team.disband.description");
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
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (!getIslands().getTeamLeader(getWorld(), targetUUID).equals(targetUUID)) {
            user.sendMessage("commands.admin.team.disband.use-disband-leader", "[leader]", getPlayers().getName(getIslands().getTeamLeader(getWorld(), targetUUID)));
            return false;
        }
        // Disband team
        getIslands().getMembers(getWorld(), targetUUID).forEach(m -> {
            User.getInstance(m).sendMessage("commands.admin.team.disband.disbanded");
            // The leader gets to keep the island
            if (!m.equals(targetUUID)) {
                getIslands().setLeaveTeam(getWorld(), m);
            }
        });
        user.sendMessage("general.success");
        return true;
    }
}
