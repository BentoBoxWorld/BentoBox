package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Displays information about Gamemodes, Addons and versioning.
 *
 * @author tastybento
 */
public class BentoBoxReloadCommand extends ConfirmableCommand {

    /**
     * Reloads locales command
     * @param parent - command parent
     */
    public BentoBoxReloadCommand(CompositeCommand parent) {
        super(parent, "reload");
    }

    @Override
    public void setup() {
        setPermission("admin.reload");
        setDescription("commands.bentobox.reload.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.askConfirmation(user, () -> reloadLocales(user));
        return false;
    }

    /**
     * Reloads the languages
     * @param user
     */
    public void reloadLocales(User user) {
        getPlugin().getLocalesManager().reloadLanguages();
        user.sendMessage("commands.bentobox.reload.locales-reloaded");
    }

}
