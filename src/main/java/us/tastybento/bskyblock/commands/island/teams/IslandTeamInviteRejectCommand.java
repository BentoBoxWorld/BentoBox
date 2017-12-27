package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamInviteRejectCommand extends AbstractIslandTeamCommand {

    public IslandTeamInviteRejectCommand(IslandTeamInviteCommand islandTeamInviteCommand) {
        super(islandTeamInviteCommand, "reject");
    }
    
    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.team.invite.reject.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Reject /island reject
        if (inviteList.containsKey(playerUUID)) {
            // Fire event so add-ons can run commands, etc.
            IslandBaseEvent event = TeamEvent.builder()
                    .island(getIslands()
                    .getIsland(inviteList.get(playerUUID)))
                    .reason(TeamReason.REJECT)
                    .involvedPlayer(playerUUID)
                    .build();
            getPlugin().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return true;

            // Remove this player from the global invite list
            inviteList.remove(user.getUniqueId());
            user.sendMessage("commands.island.team.invite.reject.you-rejected-invite");

            User inviter = User.getInstance(inviteList.get(playerUUID));
            inviter.sendMessage("commands.island.team.invite.reject.name-rejected-your-invite", "[name]", user.getName());
        } else {
            // Someone typed /island reject and had not been invited
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
        }
        return true;
    }

}
