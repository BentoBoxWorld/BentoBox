package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.hooks.PlaceholderAPIHook;

import java.util.Optional;

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
     * Registers this placeholder on the behalf of BentoBox.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with {@code "bentobox_"} by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will be this placeholder's replacement.
     */
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        // Register it in PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(placeholder, replacer));
    }

    /**
     * Registers this placeholder on the behalf of the specified addon.
     * @param addon the addon to register this placeholder on its behalf.
     *              If null, the placeholder will be registered using {@link #registerPlaceholder(String, PlaceholderReplacer)}.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with the addon's name by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will replace the placeholder.
     */
    public void registerPlaceholder(@Nullable Addon addon, @NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, replacer);
            return;
        }
        // Register it in PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(addon, placeholder, replacer));
    }

    /**
     * Unregisters this placeholder on the behalf of BentoBox.
     * Note that if the placeholder you are trying to unregister has been registered by an addon, you should use {@link #unregisterPlaceholder(Addon, String)} instead.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
        // Unregister it from PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.unregisterPlaceholder(placeholder));
    }

    /**
     * Unregisters this placeholder on the behalf of the specified addon.
     * @param addon the addon that originally registered this placeholder.
     *              If null, this placeholder will be unregistered using {@link #unregisterPlaceholder(String)}.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@Nullable Addon addon, @NonNull String placeholder) {
        if (addon == null) {
            unregisterPlaceholder(placeholder);
            return;
        }
        // Unregister it from PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.unregisterPlaceholder(addon, placeholder));
    }

    /**
     * Returns an Optional containing the PlaceholderAPIHook instance, or an empty Optional otherwise.
     * @return Optional containing the PlaceholderAPIHook instance or an empty Optional otherwise.
     * @since 1.4.0
     */
    private Optional<PlaceholderAPIHook> getPlaceholderAPIHook() {
        return plugin.getHooks().getHook("PlaceholderAPI").map(hook -> (PlaceholderAPIHook) hook);
    }
    
    /**
     * Checks if a placeholder with this name is already registered
     * @param addon the addon, not null
     * @param placeholder - name of placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
    	return getPlaceholderAPIHook().map(h -> h.isPlaceholder(addon, placeholder)).orElse(false);
    }
}
