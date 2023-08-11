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

public class IslandTeamPromoteCommand extends CompositeCommand {

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


    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        // Check rank to use command
        Island island = getIslands().getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        // Get target
        target = getPlayers().getUser(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
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

    @Override
    public boolean execute(User user, String label, List<String> args) {
        return change(user, target);
    }

    private boolean change(User user, User target) {
        Island island = getIslands().getIsland(getWorld(), user);
        int currentRank = island.getRank(target);
        if (this.getLabel().equals("promote")) {
            int nextRank = getPlugin().getRanksManager().getRankUpValue(currentRank);
            // Stop short of owner
            if (nextRank != RanksManager.OWNER_RANK && nextRank > currentRank) {
                island.setRank(target, nextRank);
                String rankName = user.getTranslation(getPlugin().getRanksManager().getRank(nextRank));
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
            int prevRank = getPlugin().getRanksManager().getRankDownValue(currentRank);
            // Lowest is Member
            if (prevRank >= RanksManager.MEMBER_RANK && prevRank < currentRank) {
                island.setRank(target, prevRank);
                String rankName = user.getTranslation(getPlugin().getRanksManager().getRank(prevRank));
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
