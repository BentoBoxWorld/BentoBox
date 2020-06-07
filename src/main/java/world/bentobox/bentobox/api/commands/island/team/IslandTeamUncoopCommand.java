package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * Command to uncoop a player
 * @author tastybento
 *
 */
public class IslandTeamUncoopCommand extends CompositeCommand {

    public IslandTeamUncoopCommand(CompositeCommand parentCommand) {
        super(parentCommand, "uncoop");
    }

    @Override
    public void setup() {
        setPermission("island.team.coop");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.uncoop.parameters");
        setDescription("commands.island.team.uncoop.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
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
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Uncoop
        return unCoopCmd(user, targetUUID);
    }

    private boolean unCoopCmd(User user, UUID targetUUID) {
        // Player cannot uncoop themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.uncoop.cannot-uncoop-yourself");
            return false;
        }
        if (getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.team.uncoop.cannot-uncoop-member");
            return false;
        }
        User target = User.getInstance(targetUUID);
        int rank = getIslands().getIsland(getWorld(), user).getRank(target);
        if (rank != RanksManager.COOP_RANK) {
            user.sendMessage("commands.island.team.uncoop.player-not-cooped");
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            island.removeMember(targetUUID);
            user.sendMessage("commands.island.team.uncoop.success", TextVariables.NAME, target.getName());
            target.sendMessage("commands.island.team.uncoop.you-are-no-longer-a-coop-member", TextVariables.NAME, user.getName());
            // Set cooldown
            if (getSettings().getCoopCooldown() > 0 && getParent() != null) {
                getParent().getSubCommand("coop").ifPresent(subCommand ->
                subCommand.setCooldown(island.getUniqueId(), targetUUID.toString(), getSettings().getCoopCooldown() * 60));
            }
            IslandEvent.builder()
            .island(island)
            .involvedPlayer(targetUUID)
            .admin(false)
            .reason(IslandEvent.Reason.RANK_CHANGE)
            .rankChange(RanksManager.COOP_RANK, RanksManager.VISITOR_RANK)
            .build();
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
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getMemberSet().stream()
                    .filter(uuid -> island.getRank(User.getInstance(uuid)) == RanksManager.COOP_RANK)
                    .map(Bukkit::getOfflinePlayer)
                    .map(OfflinePlayer::getName).collect(Collectors.toList());

            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
