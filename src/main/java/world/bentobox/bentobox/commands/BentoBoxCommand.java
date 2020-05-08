package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class BentoBoxCommand extends CompositeCommand {

    /**
     * BentoBox main command
     */
    public BentoBoxCommand() {
        super("bentobox", "bbox");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin");
        new BentoBoxVersionCommand(this);
        new BentoBoxAboutCommand(this);
        new BentoBoxManageCommand(this);
        new BentoBoxCatalogCommand(this);
        new BentoBoxReloadCommand(this);
        new BentoBoxLocaleCommand(this);
        new BentoBoxHelpCommand(this);
        // Database names with a 2 in them are migration databases
        if (getPlugin().getSettings().getDatabaseType().name().contains("2")) {
            new BentoBoxMigrateCommand(this);
        }
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        showHelp(this, user);
        return true;
    }
}
