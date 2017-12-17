package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamUninviteCommand extends AbstractIslandTeamCommandArgument {
    
    public IslandTeamUninviteCommand() {
        super("uninvite");
    }

    @Override
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        UUID playerUUID = user.getUniqueId();
        if (!user.hasPermission(Settings.PERMPREFIX + "team")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return true;
        }
        // Can only use if you have an invite out there
        if(!inviteList.inverse().containsKey(playerUUID)) {
            return true;
        }

        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(playerUUID))
                .reason(TeamReason.UNINVITE)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        if (inviteList.inverse().containsKey(playerUUID)) {
            User invitee = User.getInstance(inviteList.inverse().get(playerUUID));
            if (invitee != null) {
                inviteList.inverse().remove(playerUUID);
                invitee.sendMessage("invite.nameHasUninvitedYou", "[name]", user.getName());
                user.sendMessage("general.success");
            }
        } else {
            user.sendMessage("help.island.invite");
        }
        return false;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        return null;
    }
}
