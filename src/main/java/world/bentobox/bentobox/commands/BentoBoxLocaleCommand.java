package world.bentobox.bentobox.commands;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

import java.util.List;

/**
 * Performs localization analysis.
 *
 * @author tastybento
 * @since 1.5.0
 */
public class BentoBoxLocaleCommand extends ConfirmableCommand {

    /**
     * Localization management command
     * @param parent command parent
     */
    public BentoBoxLocaleCommand(CompositeCommand parent) {
        super(parent, "locale");
    }

    @Override
    public void setup() {
        setPermission("admin.locale");
        setDescription("commands.bentobox.locale.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Basic operation to start
        user.sendMessage("commands.bentobox.locale.see-console");
        getPlugin().getLocalesManager().analyzeLocales(false);
        return true;
    }
}
