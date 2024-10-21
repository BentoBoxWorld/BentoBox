package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.customizable.IslandHomesPanel;

public class IslandHomesCommand extends CompositeCommand {

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
        // Check island
        if (getIslands().getIslands(getWorld(), user).isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandHomesPanel.openPanel(this, user);
        return true;
    }

}
