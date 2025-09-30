package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Command that handles promoting and demoting team members within an island team hierarchy.
 * <p>
 * Features:
 * <ul>
 *   <li>Promotes/demotes team members between ranks</li>
 *   <li>Validates permissions and rank requirements</li>
 *   <li>Prevents invalid rank changes (self-promotion, promoting above own rank)</li>
 *   <li>Fires rank change events</li>
 * </ul>
 * <p>
 * This command supports both promotion and demotion through the same code base,
 * differentiating behavior based on the command label ("promote" or "demote").
 * Rank changes are restricted:
 * <ul>
 *   <li>Promotions stop short of Owner rank</li>
 *   <li>Demotions cannot go below Member rank</li>
 *   <li>Cannot promote/demote players of equal or higher rank</li>
 * </ul>
 */
public class IslandTeamPromoteCommand extends CompositeCommand {

    /** The target player for promotion/demotion */
    private User target;

    public IslandTeamPromoteCommand(CompositeCommand islandTeamCommand, String string) {
        super(islandTeamCommand, string);
    }

    @Override
    public void setup() {
        setPermission("island.team.promote");
        setOnlyPlayer(true);
        if (this.getLabel().equals("promote")) {
            setParametersHelp("commands.island.team.promote.parameters");
            setDescription("commands.island.team.promote.description");
        } else {
            setParametersHelp("commands.island.team.demote.parameters");
            setDescription("commands.island.team.demote.description");
        }
        this.setConfigurableRankCommand();
    }


    /**
     * Validates command execution conditions.
     * Checks:
     * <ul>
     *   <li>Correct number of arguments</li>
     *   <li>User is in a team</li>
     *   <li>User has sufficient rank</li>
     *   <li>Target exists and is team member</li>
     *   <li>Not trying to promote/demote self</li>
     *   <li>Target's rank is lower than user's</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Check if the user has a team
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
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

        // Get target
        target = getPlayers().getUser(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check that target is a member of this island
        if (!island.inTeam(target.getUniqueId())) {
            user.sendMessage("commands.island.team.promote.errors.must-be-member");
            return false;
        }
        // Check if the user is not trying to promote/ demote himself
        if (target.equals(user)) {
            if (this.getLabel().equals("promote")) {
                user.sendMessage("commands.island.team.promote.errors.cant-promote-yourself");
            } else {
                user.sendMessage("commands.island.team.demote.errors.cant-demote-yourself");
            }

            return false;
        }
        // Check that user is not trying to promote above their own rank
        // Check that user is not trying to demote ranks higher than them
        if (island.getRank(target) >= island.getRank(user)) {
            if (this.getLabel().equals("promote")) {
                user.sendMessage("commands.island.team.promote.errors.cant-promote");
            } else {
                user.sendMessage("commands.island.team.demote.errors.cant-demote");
            }
            return false;
        }

        return true;
    }

    /**
     * Handles the rank change logic.
     * For promotion:
     * - Increases rank but stays below owner
     * - Must be at least member rank
     * For demotion:
     * - Decreases rank but not below member
     * 
     * Fires IslandEvent.Reason.RANK_CHANGE on success
     * 
     * @param user command issuer
     * @param target player being promoted/demoted
     * @return true if rank change successful, false otherwise
     */
    private boolean change(User user, User target) {
        Island island = getIslands().getIsland(getWorld(), user);
        int currentRank = island.getRank(target);
        if (this.getLabel().equals("promote")) {
            int nextRank = RanksManager.getInstance().getRankUpValue(currentRank);
            // Stop short of owner
            if (nextRank < RanksManager.OWNER_RANK && currentRank >= RanksManager.MEMBER_RANK
                    && nextRank > currentRank) {
                island.setRank(target, nextRank);
                String rankName = user.getTranslation(RanksManager.getInstance().getRank(nextRank));
                user.sendMessage("commands.island.team.promote.success", TextVariables.NAME, target.getName(), TextVariables.RANK, rankName, TextVariables.DISPLAY_NAME, target.getDisplayName());
                IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(currentRank, nextRank)
                .build();
                return true;
            } else {
                user.sendMessage("commands.island.team.promote.failure");
                return false;
            }
        } else {
            // Demote
            int prevRank = RanksManager.getInstance().getRankDownValue(currentRank);
            // Lowest is Member
            if (prevRank >= RanksManager.MEMBER_RANK && prevRank < currentRank) {
                island.setRank(target, prevRank);
                String rankName = user.getTranslation(RanksManager.getInstance().getRank(prevRank));
                user.sendMessage("commands.island.team.demote.success", TextVariables.NAME, target.getName(), TextVariables.RANK, rankName, TextVariables.DISPLAY_NAME, target.getDisplayName());
                IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(currentRank, prevRank)
                .build();
                return true;
            } else {
                user.sendMessage("commands.island.team.demote.failure");
                return false;
            }
        }
    }

    /**
     * Provides tab completion for team member names.
     * Only shows members of the user's island.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user);
        if (island != null) {
            List<String> options = island.getMemberSet().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(OfflinePlayer::getName).toList();

            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
