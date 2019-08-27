package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class AdminSetrankCommand extends CompositeCommand {

    private int rankValue;
    private @Nullable UUID targetUUID;
    private RanksManager rm;

    public AdminSetrankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "setrank");
    }

    @Override
    public void setup() {
        setPermission("admin.setrank");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.setrank.parameters");
        setDescription("commands.admin.setrank.description");
        rm = getPlugin().getRanksManager();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 2) {
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
        if (!getPlugin().getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Get rank
        rankValue = rm.getRanks().entrySet().stream()
                .filter(r -> user.getTranslation(r.getKey()).equalsIgnoreCase(args.get(1))).findFirst()
                .map(Map.Entry::getValue).orElse(-999);
        if (rankValue < RanksManager.BANNED_RANK) {
            user.sendMessage("commands.admin.setrank.unknown-rank");
            return false;
        }
        if (rankValue <= RanksManager.VISITOR_RANK) {
            user.sendMessage("commands.admin.setrank.not-possible");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        User target = User.getInstance(targetUUID);
        Island island = getPlugin().getIslands().getIsland(getWorld(), targetUUID);
        int currentRank = island.getRank(target);
        island.setRank(target, rankValue);
        user.sendMessage("commands.admin.setrank.rank-set", "[from]", user.getTranslation(rm.getRank(currentRank)), "[to]", user.getTranslation(rm.getRank(rankValue)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(getPlugin().getRanksManager().getRanks().keySet().stream().map(user::getTranslation).collect(Collectors.toList()));
    }
}
