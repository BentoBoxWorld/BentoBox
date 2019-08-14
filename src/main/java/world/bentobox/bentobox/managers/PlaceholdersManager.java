package world.bentobox.bentobox.managers;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.hooks.placeholders.MVdWPlaceholderAPIHook;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.lists.GameModePlaceholder;

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
        // Register it in MVdWPlaceholderAPI
        getMVdWPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(placeholder, replacer));
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
        // Register it in MVdWPlaceholderAPI
        getMVdWPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(addon, placeholder, replacer));
    }

    /**
     * Registers default placeholders for this gamemode addon.
     * @param addon the gamemode addon to register the default placeholders too.
     * @since 1.5.0
     */
    public void registerDefaultPlaceholders(@NonNull GameModeAddon addon) {
        Arrays.stream(GameModePlaceholder.values())
        .filter(placeholder -> !isPlaceholder(addon, placeholder.getPlaceholder()))
        .forEach(placeholder -> registerPlaceholder(addon, placeholder.getPlaceholder(), new DefaultPlaceholder(addon, placeholder)));

        // TODO legacy placeholders, do not forget to remove at some point
        String prefix = addon.getDescription().getName().toLowerCase();
        Map<GameModePlaceholder, String> placeholders = new EnumMap<>(GameModePlaceholder.class);
        Arrays.stream(GameModePlaceholder.values()).forEach(placeholder -> placeholders.put(placeholder, prefix + "-" + placeholder.getPlaceholder().replace('_', '-')));

        // Register placeholders only if they have not already been registered by the addon itself
        placeholders.entrySet().stream().filter(en -> !isPlaceholder(addon, en.getValue()))
        .forEach(en -> registerPlaceholder(en.getValue(), new DefaultPlaceholder(addon, en.getKey())));
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
        // Not supported by MVdW
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
        // Not supported by MVdW
    }

    /**
     * Returns an Optional containing the PlaceholderAPIHook instance, or an empty Optional otherwise.
     * @return Optional containing the PlaceholderAPIHook instance or an empty Optional otherwise.
     * @since 1.4.0
     */
    @NonNull
    private Optional<PlaceholderAPIHook> getPlaceholderAPIHook() {
        return plugin.getHooks().getHook("PlaceholderAPI").map(hook -> (PlaceholderAPIHook) hook);
    }

    /**
     * Returns an Optional containing the MVdWPlaceholderAPIHook instance, or an empty Optional otherwise.
     * @return Optional containing the MVdWPlaceholderAPIHook instance or an empty Optional otherwise.
     * @since 1.5.0
     */
    @NonNull
    private Optional<MVdWPlaceholderAPIHook> getMVdWPlaceholderAPIHook() {
        return plugin.getHooks().getHook("MVdWPlaceholderAPI").map(hook -> (MVdWPlaceholderAPIHook) hook);
    }

    /**
     * Checks if a placeholder with this name is already registered
     * @param addon the addon, not null
     * @param placeholder - name of placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        // MVdW will always return false
        return getPlaceholderAPIHook().map(h -> h.isPlaceholder(addon, placeholder)).orElse(false);
    }

    /**
     * Replaces the placeholders in this String and returns it.
     * @param player the Player to get the placeholders for.
     * @param string the String to replace the placeholders in.
     * @return the String with placeholders replaced, or the identical String if no placeholders were available.
     * @since 1.5.0
     */
    public String replacePlaceholders(@NonNull Player player, @NonNull String string) {
        Optional<PlaceholderAPIHook> papi = getPlaceholderAPIHook();
        if (papi.isPresent()) {
            string = papi.get().replacePlaceholders(player, string);
        }

        Optional<MVdWPlaceholderAPIHook> mvdw = getMVdWPlaceholderAPIHook();
        if (mvdw.isPresent()) {
            string = mvdw.get().replacePlaceholders(player, string);
        }

        return string;
    }
}
