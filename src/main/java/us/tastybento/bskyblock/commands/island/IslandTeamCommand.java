package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.VaultHelper;

public class IslandTeamCommand extends CommandArgument {

    private static final boolean DEBUG = false;

    public IslandTeamCommand() {
        super("team");
        this.addSubCommand(new IslandTeamInviteCommand());
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;
        UUID playerUUID = player.getUniqueId();
        if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "team")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: executing team command for " + playerUUID);
        // Fire event so add-ons can run commands, etc.
        TeamEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(playerUUID))
                .reason(TeamReason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        UUID teamLeaderUUID = getTeamLeader(player);
        Set<UUID> teamMembers = getMembers(player);
        if (teamLeaderUUID.equals(playerUUID)) {
            int maxSize = Settings.maxTeamSize;
            for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
                    if (perms.getPermission().contains(Settings.PERMPREFIX + "team.maxsize.*")) {
                        maxSize = Settings.maxTeamSize;
                        break;
                    } else {
                        // Get the max value should there be more than one
                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                            } else {
                                maxSize = Math.max(maxSize, Integer.valueOf(spl[1]));
                            }
                        }
                    }
                }
                // Do some sanity checking
                if (maxSize < 1) maxSize = 1;
            }
            
            if (teamMembers.size() < maxSize) {
                sender.sendMessage(getLocale(sender).get("invite.youCanInvite").replace("[number]", String.valueOf(maxSize - teamMembers.size())));
            } else {
                sender.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.YourIslandIsFull"));
            }
        }
        sender.sendMessage(getLocale(sender).get("team.listingMembers"));
        // Display members in the list
        for (UUID m : teamMembers) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: member " + m);
            if (teamLeaderUUID.equals(m)) {
                sender.sendMessage(getLocale(sender).get("team.leader-color") + getPlayers().getName(m) + getLocale(sender).get("team.leader"));
            } else {
                sender.sendMessage(getLocale(sender).get("team.color") + getPlayers().getName(m));
            }
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
