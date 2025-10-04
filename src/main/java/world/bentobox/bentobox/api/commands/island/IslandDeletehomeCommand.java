package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island home deletion command (/island deletehome).
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement (default: Member)</li>
 *   <li>Confirmation before deletion</li>
 *   <li>Tab completion for home names</li>
 *   <li>Support for multiple islands per player</li>
 *   <li>Support for spaces in home names</li>
 * </ul>
 * <p>
 * Command usage: /island deletehome &lt;name&gt;
 * <br>
 * Permission: {@code island.deletehome}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandDeletehomeCommand extends ConfirmableCommand {

    /**
     * Deletes a home
     * @param islandCommand parent command
     */
    public IslandDeletehomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "deletehome");
    }

    @Override
    public void setup() {
        setPermission("island.deletehome");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.deletehome.parameters");
        setDescription("commands.island.deletehome.description");
        setConfigurableRankCommand();
        setDefaultCommandRank(RanksManager.MEMBER_RANK);
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Home name is provided</li>
     *   <li>Player has an island</li>
     *   <li>Player has sufficient rank</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // check command ranks
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank",
                    TextVariables.RANK, user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        return true;
    }

    /**
     * Handles the home deletion process.
     * <p>
     * Flow:
     * <ul>
     *   <li>Validates home name exists</li>
     *   <li>Shows list of homes if name is unknown</li>
     *   <li>Requests confirmation before deletion</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check if the name is known
        Map<String, Island> map = getNameIslandMap(user);
        String name = String.join(" ", args);
        if (!map.containsKey(name)) {
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            map.keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
            return false;
        }
        this.askConfirmation(user, () -> delete(map.get(name), user, name));
        return true;
    }

    /**
     * Performs the actual home deletion after confirmation.
     * 
     * @param island The island containing the home
     * @param user The user deleting the home
     * @param name The name of the home to delete
     */
    private void delete(Island island, User user, String name) {
        island.removeHome(name);
        user.sendMessage("general.success");
    }

    /**
     * Provides tab completion for home names across all of the user's islands.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";

        return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(user).keySet()), lastArg));

    }

    /**
     * Creates a mapping of home names to their respective islands.
     * Handles multiple islands and multiple homes per island.
     * 
     * @param user The user whose homes to map
     * @return Map of home names to their corresponding islands
     */
    private Map<String, Island> getNameIslandMap(User user) {
        Map<String, Island> islandMap = new HashMap<>();
        // Collect all homes from all islands the user has access to
        for (Island isle : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            // Add each home name mapped to its island
            isle.getHomes().keySet().forEach(name -> islandMap.put(name, isle));
        }
        return islandMap;
    }
}
