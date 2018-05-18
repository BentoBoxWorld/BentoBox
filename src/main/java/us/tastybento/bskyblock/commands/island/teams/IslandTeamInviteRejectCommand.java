package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.user.User;

public class IslandTeamInviteRejectCommand extends AbstractIslandTeamCommand {

    public IslandTeamInviteRejectCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "reject");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.reject.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Reject /island reject
        if (inviteList.containsKey(playerUUID)) {
            // Fire event so add-ons can run commands, etc.
            IslandBaseEvent event = TeamEvent.builder()
                    .island(getIslands()
                            .getIsland(user.getWorld(), inviteList.get(playerUUID)))
                    .reason(TeamEvent.Reason.REJECT)
                    .involvedPlayer(playerUUID)
                    .build();
            getPlugin().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            // Remove this player from the global invite list
            inviteList.remove(user.getUniqueId());
            user.sendMessage("commands.island.team.invite.reject.you-rejected-invite");

            User inviter = User.getInstance(inviteList.get(playerUUID));
            inviter.sendMessage("commands.island.team.invite.reject.name-rejected-your-invite", "[name]", user.getName());
        } else {
            // Someone typed /island reject and had not been invited
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return false;
        }
        return true;
    }

}
