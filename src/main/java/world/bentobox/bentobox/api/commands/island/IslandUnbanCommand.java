package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island unban command (/island unban).
 * <p>
 * This command allows island owners and team members to remove bans from players,
 * allowing them to visit the island again.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement</li>
 *   <li>Ban cooldown system</li>
 *   <li>Event system integration</li>
 *   <li>Tab completion for banned players</li>
 * </ul>
 * <p>
 * Permission: {@code island.ban}
 * Aliases: unban, pardon
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandUnbanCommand extends CompositeCommand {

    /**
     * Cached UUID of the player to be unbanned.
     * Set during canExecute and used in execute.
     */
    private @Nullable UUID targetUUID;

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

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Correct number of arguments</li>
     *   <li>Player has an island or is in team</li>
     *   <li>Player has sufficient rank</li>
     *   <li>Target player exists</li>
     *   <li>Not trying to unban self</li>
     *   <li>Target is actually banned</li>
     * </ul>
     */
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
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        // Get target player
        targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Player cannot unban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.unban.cannot-unban-yourself");
            return false;
        }
        if (!island.isBanned(targetUUID)) {
            user.sendMessage("commands.island.unban.player-not-banned");
            return false;
        }
        // Finished error checking - start the unbanning
        return true;
    }

    /**
     * Handles the unban process.
     * <p>
     * Process:
     * <ul>
     *   <li>Fires cancellable unban event</li>
     *   <li>Removes ban from island</li>
     *   <li>Notifies both parties</li>
     *   <li>Sets ban cooldown if configured</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        assert targetUUID != null;
        User target = User.getInstance(targetUUID);
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());

        // Run the event
        IslandBaseEvent unbanEvent = IslandEvent.builder()
                .island(island)
                .involvedPlayer(target.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.UNBAN)
                .build();
        if (unbanEvent.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(unbanEvent.isCancelled())) {
            // Unbanning was blocked due to an event cancellation.
            return false;
        }
        // Event is not cancelled
        assert island != null;
        if (island.unban(user.getUniqueId(), target.getUniqueId())) {
            user.sendMessage("commands.island.unban.player-unbanned", TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME, target.getDisplayName());
            target.sendMessage("commands.island.unban.you-are-unbanned", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
            // Set cooldown
            if (getSettings().getBanCooldown() > 0 && getParent() != null) {
                getParent().getSubCommand("ban").ifPresent(subCommand ->
                subCommand.setCooldown(island.getUniqueId(), target.getUniqueId().toString(), getSettings().getBanCooldown() * 60));
            }
            return true;
        }
        // Unbanning was blocked, fail silently.
        return false;
    }

    /**
     * Provides tab completion for currently banned player names.
     * Filters suggestions based on partial input.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getBanned().stream().map(getPlayers()::getName).toList();
            String lastArg = !args.isEmpty() ? args.getLast() : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
