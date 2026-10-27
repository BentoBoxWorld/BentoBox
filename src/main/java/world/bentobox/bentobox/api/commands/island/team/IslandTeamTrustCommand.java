package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Command to trust another player
 * @author tastybento
 *
 */
public class IslandTeamTrustCommand extends CompositeCommand {

    IslandTeamCommand itc;

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
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        int rank = island.getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        if (island.isBanned(targetUUID)) {
            user.sendMessage("commands.island.team.trust.banned", TextVariables.NAME, args.getFirst());
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
        rank = island.getRank(target);
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
        if (!canExecute(user, label, args)) return false;
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) return false;
        User target = User.getInstance(targetUUID);
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            if (getPlugin().getSettings().isInviteConfirmation()) {
                // Put the invited player (key) onto the list with inviter (value)
                // If someone else has invited a player, then this invite will overwrite the previous invite!
                itc.addInvite(Type.TRUST, user.getUniqueId(), target.getUniqueId(), island);
                user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME, target.getDisplayName());
                // Send message to online player
                target.sendMessage("commands.island.team.trust.name-has-invited-you", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
                target.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
            } else {
                if (getIslands().getMaxMembers(island, RanksManager.TRUSTED_RANK) >= 0
                && island.getMemberSet(RanksManager.TRUSTED_RANK, false).size()
                        >= getIslands().getMaxMembers(island, RanksManager.TRUSTED_RANK)) {
                    user.sendMessage("commands.island.team.trust.is-full");
                    return false;
                }

                island.setRank(target, RanksManager.TRUSTED_RANK);
                user.sendMessage("commands.island.team.trust.success", TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME, target.getDisplayName());
                target.sendMessage("commands.island.team.trust.you-are-trusted", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
                // Add history record
                island.log(new LogEntry.Builder(LogType.TRUSTED).data(targetUUID.toString(), "trusted")
                        .data(user.getUniqueId().toString(), "trusted by").build());

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
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        return Optional.of(Util.tabLimit(Util.getOnlinePlayerList(user), lastArg));
    }

}
