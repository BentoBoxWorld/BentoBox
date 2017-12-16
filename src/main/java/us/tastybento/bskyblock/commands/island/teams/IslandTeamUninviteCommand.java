package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamUninviteCommand extends AbstractIslandTeamCommandArgument {
    
    public IslandTeamUninviteCommand() {
        super("uninvite");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;
        UUID playerUUID = player.getUniqueId();
        if (!player.hasPermission(Settings.PERMPREFIX + "team")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }
        // Can only use if you have an invite out there
        if(!inviteList.inverse().containsKey(playerUUID)) {
            return true;
        }

        // Fire event so add-ons can run commands, etc.
        TeamEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(playerUUID))
                .reason(TeamReason.UNINVITE)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        if (inviteList.inverse().containsKey(playerUUID)) {
            Player invitee = plugin.getServer().getPlayer(inviteList.inverse().get(playerUUID));
            if (invitee != null) {
                inviteList.inverse().remove(playerUUID);
                invitee.sendMessage(ChatColor.RED + getLocale(invitee.getUniqueId()).get("invite.nameHasUninvitedYou").replace("[name]", player.getName()));
                player.sendMessage(ChatColor.GREEN + getLocale(sender).get("general.success"));
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + getLocale(sender).get("help.island.invite"));
        }
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
