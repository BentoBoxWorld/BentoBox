package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles the island lock command (/island lock).
 * <p>
 * This command allows island owners and members with the appropriate rank to lock or unlock
 * their island. When locking, all visitors currently on the island are expelled.
 * <p>
 * Features:
 * <ul>
 *   <li>Toggles the LOCK flag on the island</li>
 *   <li>Expels all non-member visitors when locking</li>
 *   <li>Configurable rank requirement</li>
 *   <li>Event system integration</li>
 * </ul>
 * <p>
 * Permission nodes:
 * <ul>
 *   <li>{@code island.lock} - Base permission</li>
 * </ul>
 *
 * @author tastybento
 * @since 3.11.0
 */
public class IslandLockCommand extends CompositeCommand {

    /**
     * Cached island instance for the command execution.
     * Set during canExecute and used in execute.
     */
    private @Nullable Island island;

    public IslandLockCommand(CompositeCommand islandCommand) {
        super(islandCommand, "lock");
    }

    @Override
    public void setup() {
        setPermission("island.lock");
        setOnlyPlayer(true);
        setDescription("commands.island.lock.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        island = getIslands().getIsland(getWorld(), user);
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        int rank = island.getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Defensive safety check in case execute is called without canExecute
        if (island == null) {
            return false;
        }

        // Check the current lock state: locked when flag rank > VISITOR_RANK
        boolean isLocked = island.getFlag(Flags.LOCK) > RanksManager.VISITOR_RANK;

        if (isLocked) {
            // Unlock the island
            island.setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
            user.sendMessage("commands.island.lock.unlocked");
        } else {
            // Lock the island
            island.setFlag(Flags.LOCK, RanksManager.MEMBER_RANK);
            user.sendMessage("commands.island.lock.locked");
            // Expel all non-member visitors from the island
            expelVisitors(island);
        }

        // Fire lock event
        IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .reason(IslandEvent.Reason.LOCK)
                .admin(false)
                .location(user.getLocation())
                .build();

        return true;
    }

    /**
     * Expels all non-team members currently on the island by teleporting them away.
     * Non-members with an island or team are sent home; others are sent to spawn.
     *
     * @param island the island to expel visitors from
     */
    private void expelVisitors(Island island) {
        island.getPlayersOnIsland().stream()
                .filter(p -> !island.inTeam(p.getUniqueId()))
                .forEach(p -> {
                    User visitor = User.getInstance(p);
                    visitor.sendMessage("commands.island.lock.you-are-locked-out");
                    if (getIslands().hasIsland(getWorld(), p.getUniqueId())
                            || getIslands().inTeam(getWorld(), p.getUniqueId())) {
                        getIslands().homeTeleportAsync(getWorld(), p);
                    } else if (getIslands().getSpawn(getWorld()).isPresent()) {
                        getIslands().spawnTeleport(getWorld(), p);
                    }
                });
    }
}
