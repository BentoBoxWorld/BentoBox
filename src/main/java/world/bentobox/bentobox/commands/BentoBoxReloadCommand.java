package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Reloads addons and localization.
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
        this.askConfirmation(user, () -> {
            // Reload addons
            getPlugin().getAddonsManager().reloadAddons();
            user.sendMessage("commands.bentobox.reload.addons-reloaded");

            // Reload locales
            getPlugin().getLocalesManager().reloadLanguages();
            user.sendMessage("commands.bentobox.reload.locales-reloaded");
        });
        return false;
    }
}
