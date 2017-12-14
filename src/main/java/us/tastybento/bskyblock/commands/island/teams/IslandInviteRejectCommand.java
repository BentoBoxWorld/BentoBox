package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;

public class IslandInviteRejectCommand extends AbstractIslandTeamCommandArgument {

    public IslandInviteRejectCommand() {
        super("reject");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
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
            inviteList.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + getLocale(playerUUID).get("reject.youHaveRejectedInvitation"));
            // If the leader is online tell them directly
            // about the rejection
            if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
                Bukkit.getPlayer(inviteList.get(playerUUID)).sendMessage(
                        ChatColor.RED + getLocale(playerUUID).get("reject.nameHasRejectedInvite").replace("[name]", player.getName()));
            }

        } else {
            // Someone typed /island reject and had not been invited
            player.sendMessage(ChatColor.RED + getLocale(playerUUID).get("reject.youHaveNotBeenInvited"));
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
