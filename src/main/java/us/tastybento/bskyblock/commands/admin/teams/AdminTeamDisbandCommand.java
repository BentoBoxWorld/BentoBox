package us.tastybento.bskyblock.commands.admin.teams;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class AdminTeamDisbandCommand extends CompositeCommand {

    public AdminTeamDisbandCommand(CompositeCommand parent) {
        super(parent, "disband");
    }
    
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.team");
        setParameters("commands.admin.team.disband.parameters");
        setDescription("commands.admin.team.disband.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();

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
        if (!getIslands().hasIsland(world, targetUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().inTeam(world, targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (!getIslands().getTeamLeader(world, targetUUID).equals(targetUUID)) {
            user.sendMessage("commands.admin.team.disband.use-disband-leader", "[leader]", getPlayers().getName(getIslands().getTeamLeader(world, targetUUID)));
            return false;
        }
        // Disband team
        getIslands().getMembers(world, targetUUID).forEach(m -> {
            User.getInstance(m).sendMessage("commands.admin.team.disband.disbanded");
            // The leader gets to keep the island
            if (!m.equals(targetUUID)) {
                getIslands().setLeaveTeam(world, m);
            }
        });
        user.sendMessage("general.success");
        return true;
    }
}
