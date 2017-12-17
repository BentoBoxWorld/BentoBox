package us.tastybento.bskyblock.commands.island.teams;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandTeamPromoteCommand extends AbstractIslandTeamCommandArgument {

    public IslandTeamPromoteCommand() {
        super("promote", "makeleader");
    }

    @Override
    public boolean execute(User user, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
        UUID playerUUID = user.getUniqueId();
        // Can use if in a team
        boolean inTeam = plugin.getPlayers().inTeam(playerUUID);
        UUID teamLeaderUUID = plugin.getIslands().getTeamLeader(playerUUID);
        if (!(inTeam && teamLeaderUUID.equals(playerUUID))) {
            return true;
        }
        plugin.getLogger().info("DEBUG: arg[0] = " + args[0]);
        UUID targetUUID = getPlayers().getUUID(args[0]);
        if (targetUUID == null) {
            user.sendMessage(ChatColor.RED + "general.errors.unknown-player");
            return true;
        }
        if (!getPlayers().inTeam(playerUUID)) {
            user.sendMessage(ChatColor.RED + "makeleader.errorYouMustBeInTeam");
            return true;
        }
        if (!teamLeaderUUID.equals(playerUUID)) {
            user.sendMessage(ChatColor.RED + "makeleader.errorNotYourIsland");
            return true;
        }
        if (targetUUID.equals(playerUUID)) {
            user.sendMessage(ChatColor.RED + "makeleader.errorGeneralError");
            return true;
        }
        if (!plugin.getIslands().getMembers(playerUUID).contains(targetUUID)) {
            user.sendMessage(ChatColor.RED + "makeleader.errorThatPlayerIsNotInTeam");
            return true;
        }
        // Fire event so add-ons can run commands, etc.
        TeamEvent event = TeamEvent.builder().island(getIslands().getIsland(playerUUID)).reason(TeamReason.MAKELEADER).involvedPlayer(targetUUID).build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;

        // target is the new leader
        getIslands().getIsland(playerUUID).setOwner(targetUUID);
        user.sendMessage(ChatColor.GREEN
                + "makeleader.nameIsNowTheOwner", "[name]", getPlayers().getName(targetUUID));

        // Check if online
        User target = User.getInstance(targetUUID);
        if (target == null) {
            // TODO offline messaging
            //plugin.getMessages().setMessage(targetPlayer, "makeleader.youAreNowTheOwner"));

        } else {
            // Online
            plugin.getServer().getPlayer(targetUUID).sendMessage(ChatColor.GREEN + getLocale(targetUUID).get("makeleader.youAreNowTheOwner"));
            // Check if new leader has a lower range permission than the island size
            boolean hasARangePerm = false;
            int range = Settings.islandProtectionRange;
            // Check for zero protection range
            Island islandByOwner = getIslands().getIsland(targetUUID);
            if (islandByOwner.getProtectionRange() == 0) {
                plugin.getLogger().warning("Player " + user.getName() + "'s island had a protection range of 0. Setting to default " + range);
                islandByOwner.setProtectionRange(range);
            }
            for (PermissionAttachmentInfo perms : target.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.range.")) {
                    if (perms.getPermission().contains(Settings.PERMPREFIX + "island.range.*")) {
                        // Ignore
                        break;
                    } else {
                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "island.range.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                plugin.getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");

                            } else {
                                hasARangePerm = true;
                                range = Math.max(range, Integer.valueOf(spl[1]));
                            }
                        }
                    }
                }
            }
            // Only set the island range if the player has a perm to override the default
            if (hasARangePerm) {
                // Do some sanity checking
                if (range % 2 != 0) {
                    range--;
                }
                // Get island range

                // Range can go up or down
                if (range != islandByOwner.getProtectionRange()) {
                    user.sendMessage("admin.SetRangeUpdated", "[number]", String.valueOf(range));
                    target.sendMessage("admin.SetRangeUpdated", "[number]", String.valueOf(range));
                    plugin.getLogger().info(
                            "Makeleader: Island protection range changed from " + islandByOwner.getProtectionRange() + " to "
                                    + range + " for " + user.getName() + " due to permission.");
                }
                islandByOwner.setProtectionRange(range);
            }
        }
        getIslands().save(true);
        return true;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        Set<String> result = new HashSet<>();
        for (UUID member : plugin.getIslands().getMembers(user.getUniqueId())) {
            result.add(plugin.getServer().getOfflinePlayer(member).getName());
        }
        return result;
    }
}