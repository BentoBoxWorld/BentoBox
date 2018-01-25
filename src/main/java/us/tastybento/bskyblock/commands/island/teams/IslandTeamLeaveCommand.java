package us.tastybento.bskyblock.commands.island.teams;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;

public class IslandTeamLeaveCommand extends AbstractIslandTeamCommand {
    
    Set<UUID> leaveSet;

    public IslandTeamLeaveCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.team.leave.description");
        leaveSet = new HashSet<>();
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!getPlayers().inTeam(user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return true;
        }
        if (!getSettings().isKickConfirmation() || leaveSet.contains(user.getUniqueId())) {
            leaveSet.remove(user.getUniqueId());
            UUID leaderUUID = getIslands().getTeamLeader(user.getUniqueId());
            if (leaderUUID != null) {
                User.getInstance(leaderUUID).sendMessage("commands.island.team.leave.left-your-island", "[player]", user.getName());
            }
            getIslands().removePlayer(user.getUniqueId());
            user.sendMessage("general.success");
        } else {
            user.sendMessage("commands.island.team.leave.type-again");
            leaveSet.add(user.getUniqueId());
            new BukkitRunnable() {

                @Override
                public void run() {
                    leaveSet.remove(user.getUniqueId());
                    user.sendMessage("general.errors.command-cancelled");
                }}.runTaskLater(getPlugin(), getSettings().getKickWait());
        }
        return true;
    }

}