package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamCommand extends AbstractIslandTeamCommandArgument {

    private static final boolean DEBUG = false;

    public IslandTeamCommand() {
        super("team");
        this.addSubCommand(new IslandTeamInviteCommand());
    }

    @Override
    public boolean execute(User user, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
        UUID playerUUID = user.getUniqueId();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: executing team command for " + playerUUID);
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(playerUUID))
                .reason(TeamReason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        UUID teamLeaderUUID = getTeamLeader(user);
        Set<UUID> teamMembers = getMembers(user);
        if (teamLeaderUUID.equals(playerUUID)) {
            int maxSize = Settings.maxTeamSize;
            for (PermissionAttachmentInfo perms : user.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
                    if (perms.getPermission().contains(Settings.PERMPREFIX + "team.maxsize.*")) {
                        maxSize = Settings.maxTeamSize;
                        break;
                    } else {
                        // Get the max value should there be more than one
                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                plugin.getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
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
                user.sendMessage("invite.youCanInvite", "[number]", String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage(ChatColor.RED + "invite.error.YourIslandIsFull");
            }
        }
        user.sendMessage("team.listingMembers");
        // Display members in the list
        for (UUID m : teamMembers) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: member " + m);
            if (teamLeaderUUID.equals(m)) {
                user.sendMessage("team.leader", "[name]", getPlayers().getName(m));
            } else {
                user.sendMessage("team.member", "[name]", getPlayers().getName(m));
            }
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        return null;
    }
}
