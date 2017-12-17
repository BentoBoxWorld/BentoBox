package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;

public class IslandInviteRejectCommand extends AbstractIslandTeamCommandArgument {

    public IslandInviteRejectCommand() {
        super("reject");
    }

    @Override
    public boolean execute(User user, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
        UUID playerUUID = user.getUniqueId();
        // Reject /island reject
        if (inviteList.containsKey(playerUUID)) {
            // Fire event so add-ons can run commands, etc.
            TeamEvent event = TeamEvent.builder()
                    .island(getIslands()
                    .getIsland(inviteList.get(playerUUID)))
                    .reason(TeamReason.REJECT)
                    .involvedPlayer(playerUUID)
                    .build();
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return true;

            // Remove this player from the global invite list
            inviteList.remove(user.getUniqueId());
            user.sendMessage(ChatColor.GREEN + "reject.youHaveRejectedInvitation");
            // If the leader is online tell them directly
            // about the rejection
            User inviter = User.getInstance(inviteList.get(playerUUID));
            if (inviter != null) {
                inviter.sendMessage(
                        ChatColor.RED + "reject.nameHasRejectedInvite", "[name]", user.getName());
            }

        } else {
            // Someone typed /island reject and had not been invited
            user.sendMessage(ChatColor.RED + "reject.youHaveNotBeenInvited");
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        return null;
    }
}
