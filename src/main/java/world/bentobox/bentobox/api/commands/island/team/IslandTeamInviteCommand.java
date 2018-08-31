package world.bentobox.bentobox.api.commands.island.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class IslandTeamInviteCommand extends CompositeCommand {

    private BiMap<UUID, UUID> inviteList;

    public IslandTeamInviteCommand(CompositeCommand islandCommand) {
        super(islandCommand, "invite");
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.description");
        inviteList = HashBiMap.create();
        setConfigurableRankCommand();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island or be in a team
        if (!getIslands().inTeam(getWorld(), user.getUniqueId()) && !getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        if (getIslands().getIsland(getWorld(), user).getRank(user) < getPlugin().getSettings().getRankCommand(getUsage())) {
            user.sendMessage("general.errors.no-permission");
            return false;
        }
        if (args.isEmpty() || args.size() > 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far
            if (inviteList.containsKey(playerUUID)) {
                OfflinePlayer inviter = getPlugin().getServer().getOfflinePlayer(inviteList.get(playerUUID));
                user.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, inviter.getName());
                return true;
            }
            // Show help
            showHelp(this, user);
            return false;
        } else  {
            // Only online players can be invited
            UUID invitedPlayerUUID = getPlayers().getUUID(args.get(0));
            if (invitedPlayerUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return false;
            }
            User invitedPlayer = User.getInstance(invitedPlayerUUID);
            if (!invitedPlayer.isOnline()) {
                user.sendMessage("general.errors.offline-player");
                return false;
            }
            // Player cannot invite themselves
            if (playerUUID.equals(invitedPlayerUUID)) {
                user.sendMessage("commands.island.team.invite.errors.cannot-invite-self");
                return false;
            }
            // Check cool down
            if (getSettings().getInviteCooldown() > 0 && checkCooldown(user, invitedPlayerUUID)) {
                return false;
            }
            // Player cannot invite someone already on a team
            if (getIslands().inTeam(getWorld(), invitedPlayerUUID)) {
                user.sendMessage("commands.island.team.invite.errors.already-on-team");
                return false;
            }
            return invite(user,invitedPlayer);
        }
    }

    private boolean invite(User user, User invitedPlayer) {
        Set<UUID> teamMembers = getMembers(getWorld(), user);
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
                    .island(getIslands().getIsland(getWorld(), user.getUniqueId()))
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
            user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, invitedPlayer.getName());
            // Send message to online player
            invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, user.getName());
            invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getLabel());
            if (getIslands().hasIsland(getWorld(), invitedPlayer.getUniqueId())) {
                invitedPlayer.sendMessage("commands.island.team.invite.you-will-lose-your-island");
            }
            return true;
        } else {
            user.sendMessage("commands.island.team.invite.errors.island-is-full");
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

    /**
     * Order is Invited, Inviter
     * @return the inviteList
     */
    public BiMap<UUID, UUID> getInviteList() {
        return inviteList;
    }

    /**
     * Gets the maximum team size for this player in this game based on the permission or the world's setting
     * @param user - user
     * @return max team size of user
     */
    public int getMaxTeamSize(User user) {
        return Util.getPermValue(user.getPlayer(), getPermissionPrefix() + "team.maxsize.", getIWM().getMaxTeamSize(getWorld()));
    }

}
