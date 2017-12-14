package us.tastybento.bskyblock.commands.island;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class IslandTeamInviteCommand extends CommandArgument {
    /**
     * Invite list - invited player name string (key), inviter name string
     * (value)
     */
    private final BiMap<UUID, UUID> inviteList = HashBiMap.create();


    public IslandTeamInviteCommand() {
        super("invite");
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
        // Player issuing the command must have an island
        if (!getPlayers().hasIsland(playerUUID)) {
            // If the player is in a team, they are not the leader
            if (getPlayers().inTeam(playerUUID)) {
                player.sendMessage(ChatColor.RED +  getLocale(sender).get("general.errors.not-leader"));
            }
            player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.YouMustHaveIslandToInvite"));
        }
        if (args.length == 0 || args.length > 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
            //TODO
            if (inviteList.containsKey(playerUUID)) {
                OfflinePlayer inviter = plugin.getServer().getOfflinePlayer(inviteList.get(playerUUID));
                player.sendMessage(ChatColor.GOLD + getLocale(sender).get("invite.nameHasInvitedYou").replace("[name]", inviter.getName()));
            } else {
                player.sendMessage(ChatColor.GOLD + getLocale(sender).get("help.island.invite"));
            }
            return true;
        }
        if (args.length == 1) {
            // Only online players can be invited
            UUID invitedPlayerUUID = getPlayers().getUUID(args[0]);
            if (invitedPlayerUUID == null) {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.offline-player"));
                return true;
            }
            Player invitedPlayer = plugin.getServer().getPlayer(invitedPlayerUUID);
            if (invitedPlayer == null) {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.offline-player"));
                return true;
            }
            // Player cannot invite themselves
            if (playerUUID.equals(invitedPlayerUUID)) {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.YouCannotInviteYourself"));
                return true;
            }
            // Check if this player can be invited to this island, or
            // whether they are still on cooldown
            long time = getPlayers().getInviteCoolDownTime(invitedPlayerUUID, getIslands().getIslandLocation(playerUUID));
            if (time > 0 && !player.isOp()) {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.CoolDown").replace("[time]", String.valueOf(time)));
                return true;
            }
            // Player cannot invite someone already on a team
            if (getPlayers().inTeam(invitedPlayerUUID)) {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.ThatPlayerIsAlreadyInATeam"));
                return true;
            }
            Set<UUID> teamMembers = getMembers(player);
            // Check if player has space on their team
            int maxSize = Settings.maxTeamSize;
            // Dynamic team sizes with permissions
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
                // If that player already has an invite out then retract it.
                // Players can only have one invite one at a time - interesting
                if (inviteList.containsValue(playerUUID)) {
                    inviteList.inverse().remove(playerUUID);
                    player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.removingInvite"));
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
                player.sendMessage(getLocale(sender).get("invite.inviteSentTo").replace("[name]", args[0]));
                // Send message to online player
                Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.GOLD + getLocale(invitedPlayerUUID).get("invite.nameHasInvitedYou").replace("[name]", player.getName()));
                Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.GOLD + 
                        "/" + getLabel() + " [accept/reject]" + " " + getLocale(invitedPlayerUUID).get("invite.toAcceptOrReject"));
                if (getPlayers().hasIsland(invitedPlayerUUID)) {
                    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + getLocale(invitedPlayerUUID).get("invite.warningYouWillLoseIsland"));
                }
            } else {
                player.sendMessage(ChatColor.RED + getLocale(sender).get("invite.error.YourIslandIsFull"));
            }
        }
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0 || !(sender instanceof Player)) {
            // Don't show every player on the server. Require at least the first letter
            return null;
        }
        return new HashSet<>(Util.getOnlinePlayerList((Player) sender));
    }
}
