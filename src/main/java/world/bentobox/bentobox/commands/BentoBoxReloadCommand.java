package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.commands.reload.BentoBoxReloadLocalesCommand;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

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
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.reload");
        setParametersHelp("commands.bentobox.reload.parameters");
        setDescription("commands.bentobox.reload.description");

        new BentoBoxReloadLocalesCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.askConfirmation(user, user.getTranslation("commands.bentobox.reload.warning"), () -> {

                // Clear placeholders that this reload will rebuild. Addon-owned
                // placeholders (e.g. from the Level addon) are intentionally
                // preserved because reload does not re-invoke addons (#2930).
                PlaceholdersManager pm = getPlugin().getPlaceholdersManager();
                pm.unregisterAll();
                getPlugin().getAddonsManager().getGameModeAddons().forEach(pm::unregisterAll);

                // Close all open panels
                PanelListenerManager.closeAllPanels();
                // Clear all template panels.
                TemplateReader.clearPanels();

                // Reload settings
                getPlugin().loadSettings();
                user.sendMessage("commands.bentobox.reload.settings-reloaded");

                // Reload locales
                getPlugin().getLocalesManager().reloadLanguages();
                user.sendMessage("commands.bentobox.reload.locales-reloaded");

                // Register new default gamemode placeholders
                getPlugin().getAddonsManager().getGameModeAddons().forEach(pm::registerDefaultPlaceholders);

            });
        } else {
            showHelp(this, user);
        }
        return true;
    }
}
