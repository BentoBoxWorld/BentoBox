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
 * Deletes a home
 * @author tastybento
 *
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
                    TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        return true;
    }

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


    private void delete(Island island, User user, String name) {
        island.removeHome(name);
        user.sendMessage("general.success");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";

        return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(user).keySet()), lastArg));

    }

    private Map<String, Island> getNameIslandMap(User user) {
        Map<String, Island> islandMap = new HashMap<>();
        for (Island isle : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            // Add homes.
            isle.getHomes().keySet().forEach(name -> islandMap.put(name, isle));
        }
        return islandMap;

    }

}
