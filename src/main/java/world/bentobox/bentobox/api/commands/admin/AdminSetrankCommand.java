package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminSetrankCommand extends CompositeCommand {

    private int rankValue;
    private @Nullable UUID targetUUID;
    private @Nullable UUID ownerUUID;
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
        if (args.size() != 2 && args.size() != 3) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
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

        if (args.size() == 2) {
            // We want to change the player's rank on the island he is part of.

            // Check if the target is part of an island
            if (!getIslands().hasIsland(getWorld(), targetUUID) && !getPlugin().getIslands().inTeam(getWorld(), targetUUID)) {
                user.sendMessage("general.errors.player-has-no-island");
                return false;
            }
        } else {
            // We want to change the player's rank on the island of the specified owner.

            ownerUUID = getPlayers().getUUID(args.get(2));
            if (ownerUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(2));
                return false;
            }

            if (!getPlugin().getIslands().hasIsland(getWorld(), ownerUUID)) {
                user.sendMessage("general.errors.player-is-not-owner", TextVariables.NAME, args.get(2));
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        User target = User.getInstance(targetUUID);
        Island island;
        if (ownerUUID != null) {
            island = getIslands().getIsland(getWorld(), ownerUUID);
        } else {
            island = getIslands().getIsland(getWorld(), targetUUID);
        }
        int currentRank = island.getRank(target);
        island.setRank(target, rankValue);
        IslandEvent.builder()
        .island(island)
        .involvedPlayer(targetUUID)
        .admin(true)
        .reason(IslandEvent.Reason.RANK_CHANGE)
        .rankChange(currentRank, rankValue)
        .build();

        String ownerName;
        if (ownerUUID != null) {
            ownerName = getPlayers().getName(ownerUUID);
        } else {
            ownerName = target.getName();
        }
        user.sendMessage("commands.admin.setrank.rank-set",
                "[from]", user.getTranslation(rm.getRank(currentRank)),
                "[to]", user.getTranslation(rm.getRank(rankValue)),
                TextVariables.NAME, ownerName);
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // Return the player names
        if (args.size() == 2) {
            return Optional.of(Util.getOnlinePlayerList(user));
        }

        // Return the ranks
        if (args.size() == 3) {
            return Optional.of(getPlugin().getRanksManager().getRanks()
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > RanksManager.VISITOR_RANK)
                    .map(entry -> user.getTranslation(entry.getKey())).collect(Collectors.toList()));
        }

        // Return the player names again for the optional island owner argument
        if (args.size() == 4) {
            return Optional.of(Util.getOnlinePlayerList(user));
        }

        return Optional.empty();
    }
}
