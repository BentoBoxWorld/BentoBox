package world.bentobox.bentobox.api.commands.admin;

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

/**
 * @author tastybento
 *
 */
public class AdminGetRankCommand extends CompositeCommand {

    public AdminGetRankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "getrank");
    }

    @Override
    public void setup() {
        setPermission("admin.setrank");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.getrank.parameters");
        setDescription("commands.admin.getrank.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Get rank
        RanksManager rm = getPlugin().getRanksManager();
        User target = User.getInstance(targetUUID);
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        int currentRank = island.getRank(target);
        user.sendMessage("commands.admin.getrank.rank-is", TextVariables.RANK, user.getTranslation(rm.getRank(currentRank)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
    }
}
