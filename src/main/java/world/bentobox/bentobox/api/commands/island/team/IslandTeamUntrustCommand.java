package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
 * Command to untrust a player
 * 
 * @author tastybento
 *
 */
public class IslandTeamUntrustCommand extends CompositeCommand {

    public IslandTeamUntrustCommand(CompositeCommand parentCommand) {
        super(parentCommand, "untrust");
    }

    @Override
    public void setup() {
        setPermission("island.team.trust");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.untrust.parameters");
        setDescription("commands.island.team.untrust.description");
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
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // untrust
        return unTrustCmd(user, targetUUID);
    }

    protected boolean unTrustCmd(User user, UUID targetUUID) {
        // Player cannot untrust themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.untrust.cannot-untrust-yourself");
            return false;
        }
        if (getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()).getMemberSet().contains(targetUUID)) {
            user.sendMessage("commands.island.team.untrust.cannot-untrust-member");
            return false;
        }
        User target = User.getInstance(targetUUID);
        int rank = getIslands().getIsland(getWorld(), user).getRank(target);
        if (rank != RanksManager.TRUSTED_RANK) {
            user.sendMessage("commands.island.team.untrust.player-not-trusted");
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            getIslands().removePlayer(island, targetUUID);
            user.sendMessage("commands.island.team.untrust.success", TextVariables.NAME, target.getName(),
                    TextVariables.DISPLAY_NAME, target.getDisplayName());
            target.sendMessage("commands.island.team.untrust.you-are-no-longer-trusted", TextVariables.NAME,
                    user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
            // Set cooldown
            if (getSettings().getTrustCooldown() > 0 && getParent() != null) {
                getParent().getSubCommand("trust").ifPresent(subCommand -> subCommand.setCooldown(island.getUniqueId(),
                        targetUUID.toString(), getSettings().getTrustCooldown() * 60));
            }
            IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(false)
                    .reason(IslandEvent.Reason.RANK_CHANGE)
                    .rankChange(RanksManager.TRUSTED_RANK, RanksManager.VISITOR_RANK).build();
            return true;
        } else {
            // Should not happen
            user.sendMessage("general.errors.general");
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getMembers().entrySet().stream()
                    .filter(e -> e.getValue() == RanksManager.TRUSTED_RANK)
                    .map(e -> Bukkit.getOfflinePlayer(e.getKey())).map(OfflinePlayer::getName).toList();
            String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
