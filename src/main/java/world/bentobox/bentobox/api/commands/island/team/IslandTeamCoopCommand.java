package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Command to coop another player
 * @author tastybento
 *
 */
public class IslandTeamCoopCommand extends CompositeCommand {

    public IslandTeamCoopCommand(CompositeCommand parentCommand) {
        super(parentCommand, "coop");
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
        return (getSettings().getCoopCooldown() <= 0 || !checkCooldown(user, targetUUID)) && coopCmd(user, targetUUID);
    }

    private boolean coopCmd(User user, UUID targetUUID) {
        // Player cannot coop themselves
        if (user.getUniqueId().equals(targetUUID)) {
            user.sendMessage("commands.island.team.coop.cannot-coop-yourself");
            return false;
        }
        if (getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.team.coop.already-has-rank");
            return false;
        }
        User target = User.getInstance(targetUUID);
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            island.setRank(target, RanksManager.COOP_RANK);
            user.sendMessage("general.success");
            target.sendMessage("commands.island.team.coop.you-are-a-coop-member", TextVariables.NAME, user.getName());
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
}
