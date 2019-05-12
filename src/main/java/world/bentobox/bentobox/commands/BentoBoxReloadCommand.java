package world.bentobox.bentobox.commands;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        setParametersHelp("commands.bentobox.reload.parameters");
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
            Optional<Addon> addon = getPlugin().getAddonsManager().getAddonByName(args.get(0));
            if (!addon.isPresent()) {
                user.sendMessage("commands.bentobox.reload.unknown-addon");
                return false;
            }

            this.askConfirmation(user, () -> {
                user.sendMessage("commands.bentobox.reload.addon", TextVariables.NAME, addon.get().getDescription().getName());
                addon.ifPresent(getPlugin().getAddonsManager()::reloadAddon);
                user.sendMessage("commands.bentobox.reload.addon-reloaded", TextVariables.NAME, addon.get().getDescription().getName());
            });
        } else {
            showHelp(this, user);
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(getPlugin().getAddonsManager().getAddons().stream().map(a -> a.getDescription().getName()).collect(Collectors.toList()));
    }
}
