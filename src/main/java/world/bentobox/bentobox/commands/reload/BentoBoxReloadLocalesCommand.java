package world.bentobox.bentobox.commands.reload;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.commands.BentoBoxReloadCommand;
import world.bentobox.bentobox.listeners.PanelListenerManager;

/**
 * Reloads locales files.
 *
 * @author Poslovitch
 * @since 1.13.0
 */
public class BentoBoxReloadLocalesCommand extends CompositeCommand {

    public BentoBoxReloadLocalesCommand(BentoBoxReloadCommand parent) {
        super(parent, "locales");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.reload");
        setDescription("commands.bentobox.reload.locales.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            // Close all open panels
            PanelListenerManager.closeAllPanels();

            // Reload locales
            getPlugin().getLocalesManager().reloadLanguages();
            user.sendMessage("commands.bentobox.reload.locales-reloaded");
        } else {
            showHelp(this, user);
        }
        return true;
    }
}
