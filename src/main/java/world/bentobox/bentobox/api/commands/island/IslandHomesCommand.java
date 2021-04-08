package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class IslandHomesCommand extends ConfirmableCommand {

    private @Nullable Island island;

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
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null || island.getOwner() == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("commands.island.sethome.homes-are");
        island.getHomes().keySet().stream().filter(s -> !s.isEmpty())
        .forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
        return true;
    }

}
