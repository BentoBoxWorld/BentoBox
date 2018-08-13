package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class IslandTeamCommand extends CompositeCommand {

    private IslandTeamInviteCommand inviteCommand;

    public IslandTeamCommand(CompositeCommand parent) {
        super(parent, "team");
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.description");
        // Register commands
        inviteCommand = new IslandTeamInviteCommand(this);
        new IslandTeamLeaveCommand(this);
        new IslandTeamSetownerCommand(this);
        new IslandTeamKickCommand(this);
        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
        new IslandTeamCoopCommand(this);
        new IslandTeamUncoopCommand(this);
        new IslandTeamTrustCommand(this);
        new IslandTeamUntrustCommand(this);
        new IslandTeamPromoteCommand(this, "promote");
        new IslandTeamPromoteCommand(this, "demote");
        
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Player issuing the command must have an island
        UUID teamLeaderUUID = getTeamLeader(getWorld(), user);
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
        Set<UUID> teamMembers = getMembers(getWorld(), user);
        if (teamLeaderUUID.equals(playerUUID)) {
            int maxSize = inviteCommand.getMaxTeamSize(user);
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", TextVariables.NUMBER, String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        // Show members of island
        getIslands().getIsland(getWorld(), playerUUID).showMembers(getPlugin(), user, getWorld());
        return true;
    }


    private boolean fireEvent(User user) {
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(getWorld(), user.getUniqueId()))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId())
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    /**
     * @return the inviteCommand
     */
    public IslandTeamInviteCommand getInviteCommand() {
        return inviteCommand;
    }

}
