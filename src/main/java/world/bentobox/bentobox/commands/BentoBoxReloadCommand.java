package world.bentobox.bentobox.commands;

import java.util.List;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.commands.reload.BentoBoxReloadLocalesCommand;
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

                // Unregister all placeholders
                getPlugin().getPlaceholdersManager().unregisterAll();

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
                getPlugin().getAddonsManager().getGameModeAddons().forEach(getPlugin().getPlaceholdersManager()::registerDefaultPlaceholders);

            });
        } else {
            showHelp(this, user);
        }
        return true;
    }
}
