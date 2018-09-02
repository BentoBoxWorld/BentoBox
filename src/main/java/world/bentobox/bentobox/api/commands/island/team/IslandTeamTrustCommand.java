package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
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

    public IslandTeamTrustCommand(CompositeCommand parentCommand) {
        super(parentCommand, "trust");
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
        if (getIslands().getIsland(getWorld(), user).getRank(user) < getPlugin().getSettings().getRankCommand(getUsage())) {
            user.sendMessage("general.errors.no-permission");
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        return (getSettings().getTrustCooldown() <= 0 || !checkCooldown(user, targetUUID)) && trustCmd(user, targetUUID);
    }

    private boolean trustCmd(User user, UUID targetUUID) {
        // Player cannot trust themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.trust.trust-in-yourself");
            return false;
        }
        if (getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.team.trust.members-trusted");
            return false;
        }
        User target = User.getInstance(targetUUID);
        int rank = getIslands().getIsland(getWorld(), user).getRank(target);
        if (rank >= RanksManager.TRUSTED_RANK) {
            user.sendMessage("commands.island.team.trust.player-already-trusted");
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            island.setRank(target, RanksManager.TRUSTED_RANK);
            user.sendMessage("general.success");
            target.sendMessage("commands.island.team.trust.you-are-trusted", TextVariables.NAME, user.getName());
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
        List<String> options  = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(user.getUniqueId()))
                .filter(p -> !island.getMemberSet().contains(p.getUniqueId()))
                .filter(p -> user.getPlayer().canSee(p))
                .map(Player::getName).collect(Collectors.toList());
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
