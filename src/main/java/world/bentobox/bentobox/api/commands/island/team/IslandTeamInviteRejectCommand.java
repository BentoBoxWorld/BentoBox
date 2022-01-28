package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class IslandTeamInviteRejectCommand extends CompositeCommand {

    private final IslandTeamCommand itc;

    public IslandTeamInviteRejectCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "reject");
        this.itc = islandTeamCommand;
    }

    @Override
    public void setup() {
        setPermission("island.team.reject");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.reject.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Reject /island reject
        if (itc.isInvited(playerUUID)) {
            // Fire event so add-ons can run commands, etc.
            IslandBaseEvent e = TeamEvent.builder()
                    .island(getIslands().getIsland(getWorld(), user.getUniqueId()))
                    .reason(TeamEvent.Reason.REJECT)
                    .involvedPlayer(playerUUID)
                    .build();
            if (e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled())) {
                return false;
            }

            Optional.ofNullable(itc.getInviter(playerUUID))
                    .map(User::getInstance)
                    .ifPresent(inviter ->
                            inviter.sendMessage("commands.island.team.invite.reject.name-rejected-your-invite", TextVariables.NAME, user.getName())
                    );

            // Remove this player from the global invite list
            itc.removeInvite(user.getUniqueId());
            user.sendMessage("commands.island.team.invite.reject.you-rejected-invite");
        } else {
            // Someone typed /island reject and had not been invited
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return false;
        }
        return true;
    }

}
