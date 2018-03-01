package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamInviteCommand extends AbstractIslandTeamCommand {

    private static final String NAME_PLACEHOLDER = "[name]";

    public IslandTeamInviteCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "invite");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.description");

        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island
        UUID teamLeaderUUID = getTeamLeader(user);
        if (teamLeaderUUID == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!(teamLeaderUUID.equals(playerUUID))) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (args.isEmpty() || args.size() > 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
            if (inviteList.containsKey(playerUUID)) {
                OfflinePlayer inviter = getPlugin().getServer().getOfflinePlayer(inviteList.get(playerUUID));
                user.sendMessage("commands.island.team.invite.name-has-invited-you", NAME_PLACEHOLDER, inviter.getName());
                return true;
            }
            // Show help
            showHelp(this, user);
            return false;
        } else  {
            // Only online players can be invited
            UUID invitedPlayerUUID = getPlayers().getUUID(args.get(0));
            if (invitedPlayerUUID == null) {
                user.sendMessage("general.errors.offline-player");
                return false;
            }
            User invitedPlayer = User.getInstance(invitedPlayerUUID);
            if (!invitedPlayer.isOnline()) {
                user.sendMessage("general.errors.offline-player");
                return false;
            }
            // Player cannot invite themselves
            if (playerUUID.equals(invitedPlayerUUID)) {
                user.sendMessage("commands.island.team.invite.cannot-invite-self");
                return false;
            }
            // Check if this player can be invited to this island, or
            // whether they are still on cooldown
            long time = getPlayers().getInviteCoolDownTime(invitedPlayerUUID, getIslands().getIslandLocation(playerUUID));
            if (time > 0 && !user.isOp()) {
                user.sendMessage("commands.island.team.invite.cooldown", "[time]", String.valueOf(time));
                return false;
            }
            // Player cannot invite someone already on a team
            if (getPlayers().inTeam(invitedPlayerUUID)) {
                user.sendMessage("commands.island.team.invite.already-on-team");
                return false;
            }
            return invite(user,invitedPlayer);
        }
    }

    private boolean invite(User user, User invitedPlayer) {
        Set<UUID> teamMembers = getMembers(user);
        // Check if player has space on their team
        int maxSize = getMaxTeamSize(user);
        if (teamMembers.size() < maxSize) {
            // If that player already has an invite out then retract it.
            // Players can only have one invite one at a time - interesting
            if (inviteList.containsValue(user.getUniqueId())) {
                inviteList.inverse().remove(user.getUniqueId());
                user.sendMessage("commands.island.team.invite.removing-invite");
            }
            // Fire event so add-ons can run commands, etc.
            IslandBaseEvent event = TeamEvent.builder()
                    .island(getIslands().getIsland(user.getUniqueId()))
                    .reason(TeamEvent.Reason.INVITE)
                    .involvedPlayer(invitedPlayer.getUniqueId())
                    .build();
            getPlugin().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            // Put the invited player (key) onto the list with inviter (value)
            // If someone else has invited a player, then this invite will overwrite the previous invite!
            inviteList.put(invitedPlayer.getUniqueId(), user.getUniqueId());
            user.sendMessage("commands.island.team.invite.invitation-sent", NAME_PLACEHOLDER, invitedPlayer.getName());
            // Send message to online player
            invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", NAME_PLACEHOLDER, user.getName());
            invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", "[label]", getLabel());
            if (getPlayers().hasIsland(invitedPlayer.getUniqueId())) {
                invitedPlayer.sendMessage("commands.island.team.invite.you-will-lose-your-island");
            }
            return true;
        } else {
            user.sendMessage("commands.island.team.invite.errors.island-is-full");
            return false;
        }
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
