package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles the island set home command (/island sethome).
 * <p>
 * This command allows players to set multiple home locations on their island,
 * with dimension-specific restrictions and confirmations.
 * <p>
 * Features:
 * <ul>
 *   <li>Multiple named home points</li>
 *   <li>Configurable maximum homes per island</li>
 *   <li>Dimension-specific restrictions (Nether/End)</li>
 *   <li>Optional confirmation dialogs</li>
 *   <li>Configurable rank requirement (default: Member)</li>
 * </ul>
 * <p>
 * Permission: {@code island.sethome}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandSethomeCommand extends ConfirmableCommand {

    /**
     * Cached island instance to avoid multiple database lookups.
     * Set during canExecute and used in execute.
     */
    private @Nullable Island island;

    public IslandSethomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "sethome");
    }

    @Override
    public void setup() {
        setPermission("island.sethome");
        setOnlyPlayer(true);
        setDescription("commands.island.sethome.description");
        setConfigurableRankCommand();
        setDefaultCommandRank(RanksManager.MEMBER_RANK);
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Player has an island</li>
     *   <li>Player is on their island</li>
     *   <li>Player has sufficient rank</li>
     *   <li>Home limit not exceeded</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null || island.getOwner() == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!island.onIsland(user.getLocation())) {
            user.sendMessage("commands.island.sethome.must-be-on-your-island");
            return false;
        }

        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        // Check number of homes

        int maxHomes = getIslands().getMaxHomes(island);
        // The + 1 is for the default home
        if (getIslands().getNumberOfHomesIfAdded(island, String.join(" ", args)) > maxHomes + 1) {
            user.sendMessage("commands.island.sethome.too-many-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
            user.sendMessage("commands.island.sethome.homes-are");
            getIslands().getIslands(getWorld(), user).forEach(is ->
            is.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s)));
            return false;
        }
        return true;
    }

    /**
     * Handles the home setting process with dimension-specific checks.
     * <p>
     * Dimension handling:
     * <ul>
     *   <li>Nether: Checks allowSetHomeInNether and confirmation settings</li>
     *   <li>End: Checks allowSetHomeInTheEnd and confirmation settings</li>
     *   <li>Overworld: No additional checks required</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        String number = String.join(" ", args);
        WorldSettings ws = getIWM().getWorldSettings(user.getWorld());
        // Check if the player is in the Nether
        if (getIWM().isNether(user.getWorld())) {
            // Check if he is (not) allowed to set his home here
            if (!ws.isAllowSetHomeInNether()) {
                user.sendMessage("commands.island.sethome.nether.not-allowed");
                return false;
            }

            // Check if a confirmation is required
            if (ws.isRequireConfirmationToSetHomeInNether()) {
                askConfirmation(user, user.getTranslation("commands.island.sethome.nether.confirmation"), () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else if (getIWM().isEnd(user.getWorld())) { // Check if the player is in the End
            // Check if he is (not) allowed to set his home here
            if (!ws.isAllowSetHomeInTheEnd()) {
                user.sendMessage("commands.island.sethome.the-end.not-allowed");
                return false;
            }

            // Check if a confirmation is required
            if (ws.isRequireConfirmationToSetHomeInTheEnd()) {
                askConfirmation(user, user.getTranslation("commands.island.sethome.the-end.confirmation"), () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else { // The player is in the Overworld, no need to run a check
            doSetHome(user, number);
        }
        return true;
    }

    /**
     * Sets the home location and notifies the user.
     * If multiple homes exist, displays a list of all homes.
     * 
     * @param user The user setting the home
     * @param name The name of the home location
     */
    private void doSetHome(User user, String name) {
        // Define a runnable as we will be using it often in the code below.
        getIslands().setHomeLocation(user, user.getLocation(), name);
        user.sendMessage("commands.island.sethome.home-set");
        if (island.getHomes().size() > 1) {
            user.sendMessage("commands.island.sethome.homes-are");
            island
            .getHomes()
            .keySet()
            .stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
        }
    }
}
