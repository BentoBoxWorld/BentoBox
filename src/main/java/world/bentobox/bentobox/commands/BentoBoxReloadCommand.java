package world.bentobox.bentobox.commands;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.PanelListenerManager;

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
        super(parent, "reload", "rl");
        setPermission("bentobox.admin.reload");
        setParametersHelp("commands.bentobox.reload.parameters");
        setDescription("commands.bentobox.reload.description");
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.askConfirmation(user, user.getTranslation("commands.bentobox.reload.warning"), () -> {
                // Close all open panels
                PanelListenerManager.closeAllPanels();

                // Reload settings
                getPlugin().loadSettings();
                user.sendMessage("commands.bentobox.reload.settings-reloaded");

                // Reload addons
                getPlugin().getAddonsManager().reloadAddons();
                user.sendMessage("commands.bentobox.reload.addons-reloaded");

                // Reload locales
                getPlugin().getLocalesManager().reloadLanguages();
                user.sendMessage("commands.bentobox.reload.locales-reloaded");

                // Fire ready event
                Bukkit.getPluginManager().callEvent(new BentoBoxReadyEvent());
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
