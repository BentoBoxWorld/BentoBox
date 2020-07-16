package world.bentobox.bentobox.api.commands.island.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class IslandTeamInviteCommand extends CompositeCommand {

    private IslandTeamCommand itc;
    private @Nullable User invitedPlayer;

    public IslandTeamInviteCommand(IslandTeamCommand parent) {
        super(parent, "invite");
        itc = parent;
    }

    @Override
    public void setup() {
        setPermission("island.team.invite");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.description");
        setConfigurableRankCommand();
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Player issuing the command must have an island or be in a team
        if (!getIslands().inTeam(getWorld(), user.getUniqueId()) && !getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        Island island = getIslands().getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }
        UUID playerUUID = user.getUniqueId();
        if (args.size() != 1) {
            // Invite label with no name, i.e., /island invite - tells the player who has invited them so far and why
            if (itc.isInvited(playerUUID)) {
                Invite invite = itc.getInvite(playerUUID);
                String name = getPlayers().getName(playerUUID);
                switch (invite.getType()) {
                case COOP:
                    user.sendMessage("commands.island.team.invite.name-has-invited-you.coop", TextVariables.NAME, name);
                    break;
                case TRUST:
                    user.sendMessage("commands.island.team.invite.name-has-invited-you.trust", TextVariables.NAME, name);
                    break;
                default:
                    user.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, name);
                    break;
                }
                return true;
            }
            // Show help
            showHelp(this, user);
            return false;
        }

        // Only online players can be invited
        UUID invitedPlayerUUID = getPlayers().getUUID(args.get(0));
        if (invitedPlayerUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        invitedPlayer = User.getInstance(invitedPlayerUUID);
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
        if (getSettings().getInviteCooldown() > 0 && checkCooldown(user, getIslands().getIsland(getWorld(), user).getUniqueId(), invitedPlayerUUID.toString())) {
            return false;
        }
        // Player cannot invite someone already on a team
        if (getIslands().inTeam(getWorld(), invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }
        if (itc.isInvited(invitedPlayerUUID) && itc.getInviter(invitedPlayerUUID).equals(user.getUniqueId()) && itc.getInvite(invitedPlayerUUID).getType().equals(Type.TEAM)) {
            // Prevent spam
            user.sendMessage("commands.island.team.invite.errors.you-have-already-invited");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Set<UUID> teamMembers = getMembers(getWorld(), user);
        // Check if player has space on their team
        int maxSize = getMaxTeamSize(user);
        if (teamMembers.size() < maxSize) {
            // If that player already has an invite out then retract it.
            // Players can only have one invite one at a time - interesting
            if (itc.isInvited(invitedPlayer.getUniqueId())) {
                itc.removeInvite(invitedPlayer.getUniqueId());
                user.sendMessage("commands.island.team.invite.removing-invite");
            }
            // Fire event so add-ons can run commands, etc.
            if (TeamEvent.builder()
                    .island(getIslands().getIsland(getWorld(), user.getUniqueId()))
                    .reason(TeamEvent.Reason.INVITE)
                    .involvedPlayer(invitedPlayer.getUniqueId())
                    .build()
                    .isCancelled()) {
                return false;
            }
            // Put the invited player (key) onto the list with inviter (value)
            // If someone else has invited a player, then this invite will overwrite the previous invite!
            itc.addInvite(Invite.Type.TEAM, user.getUniqueId(), invitedPlayer.getUniqueId());
            user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, invitedPlayer.getName());
            // Send message to online player
            invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, user.getName());
            invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
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
     * Gets the maximum team size for this player in this game based on the permission or the world's setting
     * @param user user
     * @return max team size of user
     */
    public int getMaxTeamSize(User user) {
        return user.getPermissionValue(getPermissionPrefix() + "team.maxsize", getIWM().getMaxTeamSize(getWorld()));
    }
}
