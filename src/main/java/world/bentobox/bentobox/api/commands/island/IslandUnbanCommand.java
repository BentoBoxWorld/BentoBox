package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Unban command
 * @author tastybento
 *
 */
public class IslandUnbanCommand extends CompositeCommand {

    public IslandUnbanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "unban", "pardon");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.unban.parameters");
        setDescription("commands.island.unban.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        UUID playerUUID = user.getUniqueId();
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
        // Player cannot unban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.unban.cannot-unban-yourself");
            return false;
        }
        if (!getIslands().getIsland(getWorld(), playerUUID).isBanned(targetUUID)) {
            user.sendMessage("commands.island.unban.player-not-banned");
            return false;
        }
        // Finished error checking - start the unbanning
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        User target = User.getInstance(getPlayers().getUUID(args.get(0)));
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());

        // Run the event
        IslandBaseEvent unbanEvent = IslandEvent.builder()
                .island(island)
                .involvedPlayer(target.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.UNBAN)
                .build();

        // Event is not cancelled
        if (!unbanEvent.isCancelled() && island.unban(user.getUniqueId(), target.getUniqueId())) {
            user.sendMessage("commands.island.unban.player-unbanned", TextVariables.NAME, target.getName());
            target.sendMessage("commands.island.unban.you-are-unbanned", TextVariables.NAME, user.getName());
            // Set cooldown
            if (getSettings().getBanCooldown() > 0 && getParent() != null) {
                getParent().getSubCommand("ban").ifPresent(subCommand ->
                subCommand.setCooldown(island.getUniqueId(), target.getUniqueId().toString(), getSettings().getBanCooldown() * 60));
            }
            return true;
        }
        // Unbanning was blocked, maybe due to an event cancellation. Fail silently.
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getBanned().stream().map(getPlayers()::getName).collect(Collectors.toList());
            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
