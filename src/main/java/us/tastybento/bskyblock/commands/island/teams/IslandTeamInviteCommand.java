package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamInviteCommand extends AbstractIslandTeamCommand {

    public IslandTeamInviteCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "invite");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setUsage("commands.island.team.invite.usage");
        this.setDescription("commands.island.team.invite.description");
    }

    @Override
    public void setup() {
        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island
        if (!getPlayers().hasIsland(playerUUID)) {
            // If the player is in a team, they are not the leader
            if (getPlayers().inTeam(playerUUID)) {
                user.sendMessage("general.errors.not-leader");
            }
            user.sendMessage("general.errors.no-island");
        }
        if (args.isEmpty() || args.size() > 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
            if (inviteList.containsKey(playerUUID)) {
                OfflinePlayer inviter = getPlugin().getServer().getOfflinePlayer(inviteList.get(playerUUID));
                user.sendMessage("invite.nameHasInvitedYou", "[name]", inviter.getName());
            } else {
                user.sendMessage("help.island.invite");
            }
            return true;
        } else  {
            // Only online players can be invited
            UUID invitedPlayerUUID = getPlayers().getUUID(args.get(0));
            if (invitedPlayerUUID == null) {
                user.sendMessage("general.errors.offline-player");
                return true;
            }
            User invitedPlayer = User.getInstance(invitedPlayerUUID);
            if (!invitedPlayer.isOnline()) {
                user.sendMessage("general.errors.offline-player");
                return true;
            }
            // Player cannot invite themselves
            if (playerUUID.equals(invitedPlayerUUID)) {
                user.sendMessage("invite.error.YouCannotInviteYourself");
                return true;
            }
            // Check if this player can be invited to this island, or
            // whether they are still on cooldown
            long time = getPlayers().getInviteCoolDownTime(invitedPlayerUUID, getIslands().getIslandLocation(playerUUID));
            if (time > 0 && !user.isOp()) {
                user.sendMessage("invite.error.CoolDown", "[time]", String.valueOf(time));
                return true;
            }
            // Player cannot invite someone already on a team
            if (getPlayers().inTeam(invitedPlayerUUID)) {
                user.sendMessage("invite.error.ThatPlayerIsAlreadyInATeam");
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
                                getPlugin().getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
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
                    user.sendMessage("invite.removingInvite");
                }
                // Fire event so add-ons can run commands, etc.
                IslandBaseEvent event = TeamEvent.builder()
                        .island(getIslands().getIsland(playerUUID))
                        .reason(TeamReason.INVITE)
                        .involvedPlayer(invitedPlayerUUID)
                        .build();
                getPlugin().getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return true;
                // Put the invited player (key) onto the list with inviter (value)
                // If someone else has invited a player, then this invite will overwrite the previous invite!
                inviteList.put(invitedPlayerUUID, playerUUID);
                user.sendMessage("invite.inviteSentTo", "[name]", args.get(0));
                // Send message to online player
                invitedPlayer.sendMessage("invite.nameHasInvitedYou", "[name]", user.getName());
                invitedPlayer.sendMessage("invite.toAcceptOrReject", "[label]", getLabel());
                if (getPlayers().hasIsland(invitedPlayer.getUniqueId())) {
                    invitedPlayer.sendMessage("invite.warningYouWillLoseIsland");
                }
            } else {
                user.sendMessage("invite.error.YourIslandIsFull");
            }
        }
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = (!args.isEmpty() ? args.getLast() : "");
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        options.addAll(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
