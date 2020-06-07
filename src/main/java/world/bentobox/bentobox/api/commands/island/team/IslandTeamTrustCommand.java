package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Command to trust another player
 * @author tastybento
 *
 */
public class IslandTeamTrustCommand extends CompositeCommand {

    IslandTeamCommand itc;
    private @Nullable UUID targetUUID;

    public IslandTeamTrustCommand(IslandTeamCommand parentCommand) {
        super(parentCommand, "trust");
        this.itc = parentCommand;
    }

    @Override
    public void setup() {
        setPermission("island.team.trust");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.trust.parameters");
        setDescription("commands.island.team.trust.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
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
        // Get target player
        targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check cooldown
        if (getSettings().getTrustCooldown() > 0 && checkCooldown(user, island.getUniqueId(), targetUUID.toString())) {
            return false;
        }
        // Player cannot coop themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.trust.trust-in-yourself");
            return false;
        }
        User target = User.getInstance(targetUUID);
        rank = getIslands().getIsland(getWorld(), user).getRank(target);
        if (rank >= RanksManager.TRUSTED_RANK) {
            user.sendMessage("commands.island.team.trust.player-already-trusted");
            return false;
        }
        if (itc.isInvited(targetUUID) && itc.getInviter(targetUUID).equals(user.getUniqueId()) && itc.getInvite(targetUUID).getType().equals(Type.TRUST)) {
            // Prevent spam
            user.sendMessage("commands.island.team.invite.errors.you-have-already-invited");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        User target = User.getInstance(targetUUID);
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            if (getPlugin().getSettings().isInviteConfirmation()) {
                // Put the invited player (key) onto the list with inviter (value)
                // If someone else has invited a player, then this invite will overwrite the previous invite!
                itc.addInvite(Type.TRUST, user.getUniqueId(), target.getUniqueId());
                user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, target.getName());
                // Send message to online player
                target.sendMessage("commands.island.team.trust.name-has-invited-you", TextVariables.NAME, user.getName());
                target.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
            } else {
                if (island.getMemberSet(RanksManager.TRUSTED_RANK, false).size() > getMaxTrustSize(user)) {
                    user.sendMessage("commands.island.team.trust.is-full");
                    return false;
                }

                island.setRank(target, RanksManager.TRUSTED_RANK);
                user.sendMessage("commands.island.team.trust.success", TextVariables.NAME, target.getName());
                target.sendMessage("commands.island.team.trust.you-are-trusted", TextVariables.NAME, user.getName());
            }
            return true;
        } else {
            // Should not happen
            user.sendMessage("general.errors.general");
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        String lastArg = args.get(args.size()-1);
        return Optional.of(Util.tabLimit(Util.getOnlinePlayerList(user), lastArg));
    }

    /**
     * Gets the maximum trust size for this player in this game based on the permission or the world's setting
     * @param user user
     * @return max trust size of user
     * @since 1.13.0
     */
    public int getMaxTrustSize(User user) {
        return user.getPermissionValue(getPermissionPrefix() + "trust.maxsize", getIWM().getMaxTrustSize(getWorld()));
    }
}
