package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class BentoBoxCommand extends CompositeCommand {

    /**
     * About
     * @param islandCommand - parent command
     */
    public BentoBoxCommand() {
        super("bentobox", "bbox");
    }

    @Override
    public void setup() {
        setDescription("commands.bentobox.description");
        new InfoCommand(this);
        new AboutCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }

}
