package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Reloads settings, addons and localization.
 *
 * @author tastybento
 */
public class BentoBoxReloadCommand extends ConfirmableCommand {

    /**
     * Reloads settings, addons and localization command
     * @param parent command parent
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
        if (args.isEmpty()) {
            this.askConfirmation(user, () -> {
                // Reload settings
                getPlugin().loadSettings();
                user.sendMessage("commands.bentobox.reload.settings-reloaded");

                // Reload addons
                getPlugin().getAddonsManager().reloadAddons();
                user.sendMessage("commands.bentobox.reload.addons-reloaded");

                // Reload locales
                getPlugin().getLocalesManager().reloadLanguages();
                user.sendMessage("commands.bentobox.reload.locales-reloaded");
            });
        } else if (args.size() == 1) {
            if (!getPlugin().getAddonsManager().getAddonByName(args.get(0)).isPresent()) {
                user.sendRawMessage("Unknown addon");
                return false;
            }
            this.askConfirmation(user, () -> getPlugin().getAddonsManager().getAddonByName(args.get(0)).ifPresent(getPlugin().getAddonsManager()::reloadAddon));
        } else {
            showHelp(this, user);
        }
        return true;
    }
}
