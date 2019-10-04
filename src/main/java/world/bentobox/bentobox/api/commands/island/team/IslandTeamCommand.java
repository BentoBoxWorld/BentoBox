package world.bentobox.bentobox.api.commands.island.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.InviteType;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class IslandTeamCommand extends CompositeCommand {

    /**
     * Invited list. Key is the invited party, value is the invite.
     */
    private Map<UUID, Invite> inviteList;

    private IslandTeamInviteCommand inviteCommand;

    public IslandTeamCommand(CompositeCommand parent) {
        super(parent, "team");
        inviteList = new HashMap<>();
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
        UUID ownerUUID = getOwner(getWorld(), user);
        if (ownerUUID == null) {
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
        if (ownerUUID.equals(playerUUID)) {
            int maxSize = inviteCommand.getMaxTeamSize(user);
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", TextVariables.NUMBER, String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        // Show members of island
        getIslands().getIsland(getWorld(), playerUUID).showMembers(user);
        return true;
    }

    private boolean fireEvent(User user) {
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                        .getIsland(getWorld(), user.getUniqueId()))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId())
                .build();
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    public void addInvite(InviteType type, UUID inviter, UUID invitee) {
        inviteList.put(invitee, new Invite(type, inviter, invitee));
    }

    public boolean isInvited(UUID invitee) {
        return inviteList.containsKey(invitee);
    }

    /**
     * Get whoever invited invitee
     * @param invitee - uuid
     * @return UUID of inviter, or null if invitee has not been invited
     */
    public UUID getInviter(UUID invitee) {
        return isInvited(invitee) ? inviteList.get(invitee).getInviter() : null;
    }

    /**
     * Get the invite for an invitee
     * @param invitee - UUID of invitee
     * @return invite or null if none
     */
    public Invite getInvite(UUID invitee) {
        return inviteList.get(invitee);
    }

    /**
     * Remove an invite made
     * @param uniqueId - UUID of invited user
     */
    public void removeInvite(UUID uniqueId) {
        inviteList.remove(uniqueId);

    }
}
