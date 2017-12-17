package us.tastybento.bskyblock.commands.island.teams;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamInviteCommand extends AbstractIslandTeamCommandArgument {

    public IslandTeamInviteCommand() {
        super("invite");
    }

    @Override
    public boolean execute(User user, String[] args) {
        // Check team perm and get variables set
        if (!checkTeamPerm()) return true;
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island
        if (!getPlayers().hasIsland(playerUUID)) {
            // If the player is in a team, they are not the leader
            if (getPlayers().inTeam(playerUUID)) {
                user.sendMessage(ChatColor.RED + "general.errors.not-leader");
            }
            user.sendMessage(ChatColor.RED + "invite.error.YouMustHaveIslandToInvite");
        }
        if (args.length == 0 || args.length > 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
            if (inviteList.containsKey(playerUUID)) {
                OfflinePlayer inviter = plugin.getServer().getOfflinePlayer(inviteList.get(playerUUID));
                user.sendMessage(ChatColor.GOLD + "invite.nameHasInvitedYou", "[name]", inviter.getName());
            } else {
                user.sendMessage(ChatColor.GOLD + "help.island.invite");
            }
            return true;
        }
        if (args.length == 1) {
            // Only online players can be invited
            UUID invitedPlayerUUID = getPlayers().getUUID(args[0]);
            if (invitedPlayerUUID == null) {
                user.sendMessage(ChatColor.RED + "general.errors.offline-player");
                return true;
            }
            User invitedPlayer = User.getInstance(invitedPlayerUUID);
            if (!invitedPlayer.isOnline()) {
                user.sendMessage(ChatColor.RED + "general.errors.offline-player");
                return true;
            }
            // Player cannot invite themselves
            if (playerUUID.equals(invitedPlayerUUID)) {
                user.sendMessage(ChatColor.RED + "invite.error.YouCannotInviteYourself");
                return true;
            }
            // Check if this player can be invited to this island, or
            // whether they are still on cooldown
            long time = getPlayers().getInviteCoolDownTime(invitedPlayerUUID, getIslands().getIslandLocation(playerUUID));
            if (time > 0 && !user.isOp()) {
                user.sendMessage(ChatColor.RED + "invite.error.CoolDown", "[time]", String.valueOf(time));
                return true;
            }
            // Player cannot invite someone already on a team
            if (getPlayers().inTeam(invitedPlayerUUID)) {
                user.sendMessage(ChatColor.RED + "invite.error.ThatPlayerIsAlreadyInATeam");
                return true;
            }
            Set<UUID> teamMembers = getMembers(user);
            // Check if player has space on their team
            int maxSize = Settings.maxTeamSize;
            // Dynamic team sizes with permissions
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
                // If that player already has an invite out then retract it.
                // Players can only have one invite one at a time - interesting
                if (inviteList.containsValue(playerUUID)) {
                    inviteList.inverse().remove(playerUUID);
                    user.sendMessage(ChatColor.RED + "invite.removingInvite");
                }
                // Fire event so add-ons can run commands, etc.
                TeamEvent event = TeamEvent.builder()
                        .island(getIslands().getIsland(playerUUID))
                        .reason(TeamReason.INVITE)
                        .involvedPlayer(invitedPlayerUUID)
                        .build();
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return true;
                // Put the invited player (key) onto the list with inviter (value)
                // If someone else has invited a player, then this invite will overwrite the previous invite!
                inviteList.put(invitedPlayerUUID, playerUUID);
                user.sendMessage("invite.inviteSentTo", "[name]", args[0]);
                // Send message to online player
                invitedPlayer.sendMessage(ChatColor.GOLD + "invite.nameHasInvitedYou", "[name]", user.getName());
                invitedPlayer.sendMessage(ChatColor.GOLD + "invite.toAcceptOrReject", "[label]", getLabel());
                if (getPlayers().hasIsland(invitedPlayer.getUniqueId())) {
                    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + getLocale(invitedPlayerUUID).get("invite.warningYouWillLoseIsland"));
                }
            } else {
                user.sendMessage(ChatColor.RED + "invite.error.YourIslandIsFull");
            }
        }
        return false;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        if (args.length == 0 || !isPlayer(user)) {
            // Don't show every player on the server. Require at least the first letter
            return null;
        }
        return new HashSet<>(Util.getOnlinePlayerList(user));
    }
}
