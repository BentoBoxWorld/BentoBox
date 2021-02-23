package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Deletes a home
 * @author tastybento
 *
 */
public class IslandDeletehomeCommand extends ConfirmableCommand {

    private @Nullable Island island;

    public IslandDeletehomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "deletehome");
    }

    @Override
    public void setup() {
        setPermission("island.deletehome");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.deletehome.parameters");
        setDescription("commands.island.deletehome.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check if the name is known
        if (!getIslands().isHomeLocation(island, String.join(" ", args))) {
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("home-list-syntax", TextVariables.NAME, s));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.askConfirmation(user, () -> delete(island, user, String.join(" ", args)));
        return true;
    }


    private void delete(Island island, User user, String name) {
        island.removeHome(name);
        user.sendMessage("general.success");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            return Optional.of(Util.tabLimit(new ArrayList<>(island.getHomes().keySet()), lastArg));
        } else {
            return Optional.empty();
        }
    }
}
