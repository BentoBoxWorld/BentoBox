package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class InfoCommand extends CompositeCommand {

    /**
     * Info command
     * @param parent - command parent
     */
    public InfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    @Override
    public void setup() {
        setDescription("commands.bentobox.info.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("commands.bentobox.info.loaded-game-worlds");
        getIWM().getOverWorldNames().forEach(n -> user.sendMessage("commands.bentobox.info.game-worlds", TextVariables.NAME, n));
        user.sendMessage("commands.bentobox.info.loaded-addons");
        getPlugin().getAddonsManager()
        .getAddons()
        .forEach(a -> user.sendMessage("commands.bentobox.info.addon-syntax", TextVariables.NAME, a.getDescription().getName(),
                TextVariables.VERSION, a.getDescription().getVersion()));

        return true;
    }

}
