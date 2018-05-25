package us.tastybento.bskyblock.commands.admin.teams;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class AdminTeamAddCommand extends CompositeCommand {

    public AdminTeamAddCommand(CompositeCommand parent) {
        super(parent, "add");

    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.team");
        setParameters("commands.admin.team.add.parameters");
        setDescription("commands.admin.team.add.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();
        // If args are not right, show help
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }
        // Get leader and target
        UUID leaderUUID = getPlayers().getUUID(args.get(0));
        if (leaderUUID == null) {
            user.sendMessage("general.errors.unknown-player-name", "[name]", args.get(0));
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.get(1));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player-name", "[name]", args.get(1));
            return false;
        }
        if (!getIslands().hasIsland(world, leaderUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(world, leaderUUID) && !getIslands().getTeamLeader(world, leaderUUID).equals(leaderUUID)) {
            user.sendMessage("commands.admin.team.add.name-not-leader", "[name]", args.get(0));
            getIslands().getIsland(world, leaderUUID).showMembers(getPlugin(), user);
            return false;
        }
        if (getIslands().inTeam(world, targetUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }
        if (getIslands().hasIsland(world, targetUUID)) {
            user.sendMessage("commands.admin.team.add.name-has-island", "[name]", args.get(1));
            return false;
        }
        // Success
        User target = User.getInstance(targetUUID);
        User leader = User.getInstance(leaderUUID);
        leader.sendMessage("commands.island.team.invite.accept.name-joined-your-island", "[name]", target.getName());
        target.sendMessage("commands.island.team.invite.accept.you-joined-island", "[label]", Constants.ISLANDCOMMAND);
        getIslands().getIsland(world, leaderUUID).addMember(targetUUID);
        user.sendMessage("general.success");
        return true;

    }


}
