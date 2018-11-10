package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.hooks.PlaceholderAPIHook;

/**
 * Manages placeholder integration.
 *
 * @author Poslovitch
 */
public class PlaceholdersManager {

    private BentoBox plugin;

    public PlaceholdersManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the placeholder on the behalf on BentoBox.
     * @param placeholder the placeholder to register. It will be appended with {@code "bentobox_"} by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will replace the placeholder.
     */
    public void registerPlaceholder(String placeholder, PlaceholderReplacer replacer) {
        // Register it in PlaceholderAPI
        plugin.getHooks().getHook("PlaceholderAPI").ifPresent(hook -> ((PlaceholderAPIHook) hook).registerBentoBoxPlaceholder(placeholder, replacer));
    }

    /**
     * Registers the placeholder on the behalf of the specified addon.
     * @param placeholder the placeholder to register. It will be appended with the addon's name by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will replace the placeholder.
     */
    public void registerPlaceholder(Addon addon, String placeholder, PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, replacer);
            return;
        }
        // Register it in PlaceholderAPI
        plugin.getHooks().getHook("PlaceholderAPI").ifPresent(hook -> ((PlaceholderAPIHook) hook).registerAddonPlaceholder(addon, placeholder, replacer));
    }
}
