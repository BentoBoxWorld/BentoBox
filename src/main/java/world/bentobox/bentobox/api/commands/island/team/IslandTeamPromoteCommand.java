package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

public class IslandTeamPromoteCommand extends CompositeCommand {

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
    public boolean execute(User user, String label, List<String> args) {
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return true;
        }
        // Check rank to use command
        Island island = getIslands().getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        User target = getPlayers().getUser(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return true;
        }
        // Check if the user is not trying to promote/ demote himself
        if (target == user) {
            user.sendMessage("commands.island.team.demote.errors.cant-demote-yourself");
            return true;
        }
        if (!inTeam(getWorld(), target) || !getOwner(getWorld(), user).equals(getOwner(getWorld(), target))) {
            user.sendMessage("general.errors.not-in-team");
            return true;
        }

        return change(user, target);
    }

    private boolean change(User user, User target) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        int currentRank = island.getRank(target);
        if (this.getLabel().equals("promote")) {
            int nextRank = getPlugin().getRanksManager().getRankUpValue(currentRank);
            // Stop short of owner
            if (nextRank != RanksManager.OWNER_RANK && nextRank > currentRank) {
                getIslands().getIsland(getWorld(), user.getUniqueId()).setRank(target, nextRank);
                String rankName = user.getTranslation(getPlugin().getRanksManager().getRank(nextRank));
                user.sendMessage("commands.island.team.promote.success", TextVariables.NAME, target.getName(), TextVariables.RANK, rankName);
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
                getIslands().getIsland(getWorld(), user.getUniqueId()).setRank(target, prevRank);
                String rankName = user.getTranslation(getPlugin().getRanksManager().getRank(prevRank));
                user.sendMessage("commands.island.team.demote.success", TextVariables.NAME, target.getName(), TextVariables.RANK, rankName);
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

}
