package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.user.User;

public class IslandTeamCommand extends AbstractIslandTeamCommand {

    public IslandTeamCommand(CompositeCommand islandCommand) {
        super(islandCommand, "team");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.description");

        new IslandTeamInviteCommand(this);
        new IslandTeamLeaveCommand(this);
        // TODO: These are still in development
        //new IslandTeamPromoteCommand(this, "promote");
        //new IslandTeamPromoteCommand(this, "demote");
        new IslandTeamSetownerCommand(this);
        new IslandTeamKickCommand(this);
        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // Player issuing the command must have an island
        UUID teamLeaderUUID = getTeamLeader(user.getWorld(), user);
        if (teamLeaderUUID == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        UUID playerUUID = user.getUniqueId();
        // Fire event so add-ons can run commands, etc.
        if (fireEvent(user)) {
            // Cancelled
            return false;
        }
        Set<UUID> teamMembers = getMembers(user.getWorld(), user);
        if (teamLeaderUUID.equals(playerUUID)) {
            int maxSize = getMaxTeamSize(user);
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", "[number]", String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        // Show members of island
        getIslands().getIsland(user.getWorld(), playerUUID).showMembers(getPlugin(), user);
        return true;
    }


    private boolean fireEvent(User user) {
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(user.getWorld(), user.getUniqueId()))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId())
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

}
