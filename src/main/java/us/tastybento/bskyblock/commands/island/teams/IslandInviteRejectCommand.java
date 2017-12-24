package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.config.Settings;

public class IslandInviteRejectCommand extends AbstractIslandTeamCommand {

    public IslandInviteRejectCommand(IslandCommand islandCommand) {
        super(islandCommand, "reject");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
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
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return true;

            // Remove this player from the global invite list
            inviteList.remove(user.getUniqueId());
            user.sendMessage("reject.youHaveRejectedInvitation");
            // If the leader is online tell them directly
            // about the rejection
            User inviter = User.getInstance(inviteList.get(playerUUID));
            if (inviter != null) {
                inviter.sendMessage("reject.nameHasRejectedInvite", "[name]", user.getName());
            }

        } else {
            // Someone typed /island reject and had not been invited
            user.sendMessage("reject.youHaveNotBeenInvited");
        }
        return true;
    }

}
