package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Command to coop another player
 * 
 * @author tastybento
 *
 */
public class IslandTeamCoopCommand extends CompositeCommand {

    private final IslandTeamCommand itc;
    private @Nullable UUID targetUUID;

    public IslandTeamCoopCommand(IslandTeamCommand parentCommand) {
        super(parentCommand, "coop");
        this.itc = parentCommand;
    }

    @Override
    public void setup() {
        setPermission("island.team.coop");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.coop.parameters");
        setDescription("commands.island.team.coop.description");
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
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())
                && !getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        Island island = getIslands().getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        // Get target player
        targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check cooldown
        if (getSettings().getCoopCooldown() > 0 && checkCooldown(user, island.getUniqueId(), targetUUID.toString())) {
            return false;
        }
        // Player cannot coop themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.coop.cannot-coop-yourself");
            return false;
        }
        if (getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()).getMemberSet(RanksManager.COOP_RANK)
                .contains(targetUUID)) {
            user.sendMessage("commands.island.team.coop.already-has-rank");
            return false;
        }
        if (itc.isInvited(targetUUID) && user.getUniqueId().equals(itc.getInviter(targetUUID))
                && itc.getInvite(targetUUID) != null && itc.getInvite(targetUUID).getType().equals(Type.COOP)) {
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
                // If someone else has invited a player, then this invite will overwrite the
                // previous invite!
                itc.addInvite(Type.COOP, user.getUniqueId(), target.getUniqueId(), island);
                user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, target.getName());
                // Send message to online player
                target.sendMessage("commands.island.team.coop.name-has-invited-you", TextVariables.NAME,
                        user.getName());
                target.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL,
                        getTopLabel());
            } else {
                if (island.getMemberSet(RanksManager.COOP_RANK, false).size() >= getIslands().getMaxMembers(island,
                        RanksManager.COOP_RANK)) {
                    user.sendMessage("commands.island.team.coop.is-full");
                    return false;
                }

                island.setRank(target, RanksManager.COOP_RANK);
                user.sendMessage("commands.island.team.coop.success", TextVariables.NAME, target.getName(),
                        TextVariables.DISPLAY_NAME, target.getDisplayName());
                target.sendMessage("commands.island.team.coop.you-are-a-coop-member", TextVariables.NAME,
                        user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
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
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        return Optional.of(Util.tabLimit(Util.getOnlinePlayerList(user), lastArg));
    }

}
