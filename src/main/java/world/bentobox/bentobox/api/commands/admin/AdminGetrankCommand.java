package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminGetrankCommand extends CompositeCommand {

    private Island island;
    private @Nullable UUID targetUUID;

    public AdminGetrankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "getrank");
    }

    @Override
    public void setup() {
        setPermission("admin.getrank");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.getrank.parameters");
        setDescription("commands.admin.getrank.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        island = getIslands().getIsland(getWorld(), targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get rank
        RanksManager rm = getPlugin().getRanksManager();
        User target = User.getInstance(targetUUID);
        int currentRank = island.getRank(target);
        user.sendMessage("commands.admin.getrank.rank-is", TextVariables.RANK, user.getTranslation(rm.getRank(currentRank)));
        return true;

    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        List<String> options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
