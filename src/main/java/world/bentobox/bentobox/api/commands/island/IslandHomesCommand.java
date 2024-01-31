package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class IslandHomesCommand extends ConfirmableCommand {

    private Set<Island> islands;

    public IslandHomesCommand(CompositeCommand islandCommand) {
        super(islandCommand, "homes");
    }

    @Override
    public void setup() {
        setPermission("island.homes");
        setOnlyPlayer(true);
        setDescription("commands.island.homes.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        islands = getIslands().getIslands(getWorld(), user);
        // Check island
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("commands.island.sethome.homes-are");
        islands.forEach(island ->
        island.getHomes().keySet().stream().filter(s -> !s.isEmpty())
        .forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        List<String> result = new ArrayList<>();
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            result.addAll(island.getHomes().keySet());
        }
        return Optional.of(Util.tabLimit(result, lastArg));

    }

}
