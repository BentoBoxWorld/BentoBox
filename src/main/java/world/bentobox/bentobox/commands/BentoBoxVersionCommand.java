package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Displays information about Gamemodes, Addons and versioning.
 *
 * @author tastybento
 */
public class BentoBoxVersionCommand extends CompositeCommand {

    /**
     * Info command
     * @param parent - command parent
     */
    public BentoBoxVersionCommand(CompositeCommand parent) {
        super(parent, "version", "v", "versions", "addons");
    }

    @Override
    public void setup() {
        // Not used
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("commands.bentobox.version.loaded-game-worlds");
        getIWM().getOverWorldNames().forEach(n -> user.sendMessage("commands.bentobox.version.game-worlds", TextVariables.NAME, n));
        user.sendMessage("commands.bentobox.version.loaded-addons");
        getPlugin().getAddonsManager()
        .getAddons()
        .forEach(a -> user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion()));

        return true;
    }

}
