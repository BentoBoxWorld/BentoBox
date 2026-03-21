package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;


/**
 * Handles the island reset name command (/island resetname).
 * <p>
 * This command removes the custom name from an island, reverting it
 * to the default naming scheme. It requires the same permission as
 * the name command and appropriate rank to execute.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement</li>
 *   <li>Permission inheritance from name command</li>
 *   <li>Immediate name removal</li>
 * </ul>
 * <p>
 * Permission: {@code island.name}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandResetnameCommand extends CompositeCommand {

    public IslandResetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "resetname");
    }

    @Override
    public void setup() {
        setPermission("island.name");
        setOnlyPlayer(true);
        setDescription("commands.island.resetname.description");
    }


    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Player has an island</li>
     *   <li>Player has sufficient rank</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        Island island = getIslands().getIsland(getWorld(), user);

        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // Check command rank.
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank",
                    TextVariables.RANK, user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        return true;
    }


    /**
     * Removes the custom name from the island.
     * Sets the island name to null, which reverts it to the default naming scheme.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {

        // Resets the island name
        Objects.requireNonNull(getIslands().getIsland(getWorld(), user)).setName(null);
        user.sendMessage("commands.island.resetname.success");
        return true;
    }

}
