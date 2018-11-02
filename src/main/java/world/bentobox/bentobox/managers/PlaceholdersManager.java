package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.hooks.PlaceholderAPIHook;

public class PlaceholdersManager {

    private BentoBox plugin;

    public PlaceholdersManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    public void registerPlaceholder(String placeholder, PlaceholderReplacer replacer) {
        // Register it in PlaceholderAPI
        plugin.getHooks().getHook("PlaceholderAPI").ifPresent(hook -> {
            PlaceholderAPIHook placeholderAPIHook = (PlaceholderAPIHook) hook;
            placeholderAPIHook.registerBentoBoxPlaceholder(placeholder, replacer);
        });
    }

    public void registerPlaceholder(Addon addon, String placeholder, PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, replacer);
            return;
        }
        // Register it in PlaceholderAPI
        plugin.getHooks().getHook("PlaceholderAPI").ifPresent(hook -> {
            PlaceholderAPIHook placeholderAPIHook = (PlaceholderAPIHook) hook;
            placeholderAPIHook.registerAddonPlaceholder(addon, placeholder, replacer);
        });
    }
}
