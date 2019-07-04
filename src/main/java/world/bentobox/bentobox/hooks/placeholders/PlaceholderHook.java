package world.bentobox.bentobox.hooks.placeholders;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;

/**
 * @author Poslovitch
 * @since 1.5.0
 */
public abstract class PlaceholderHook extends Hook {

    public PlaceholderHook(@NonNull String pluginName) {
        super(pluginName, Material.NAME_TAG);
    }

    /**
     * Registers this placeholder on the behalf of BentoBox.
     * @param placeholder the placeholder to register, not null
     * @param replacer its replacement, not null
     * @since 1.5.0
     */
    public abstract void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer);

    /**
     * Registers this placeholder on the behalf of this addon.
     * @param addon the addon, not null.
     * @param placeholder the placeholder to register, not null.
     * @param replacer its replacement, not null.
     * @since 1.5.0
     */
    public abstract void registerPlaceholder(@NonNull Addon addon, @NonNull String placeholder, @NonNull PlaceholderReplacer replacer);

    /**
     * Unregisters this placeholder on the behalf of BentoBox.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.5.0
     */
    public abstract void unregisterPlaceholder(@NonNull String placeholder);

    /**
     * Unregister this placeholder on the behalf of this addon.
     * @param addon the addon, not null.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.5.0
     */
    public abstract void unregisterPlaceholder(@NonNull Addon addon, @NonNull String placeholder);

    /**
     * Checks if a placeholder with this name is already registered
     * @param addon the addon, not null
     * @param placeholder this placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.5.0
     */
    public abstract boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder);

    /**
     * Replaces the placeholders in this String and returns it.
     * @param player the Player to get the placeholders for.
     * @param string the String to replace the placeholders in.
     * @return the String with placeholders replaced, or the identical String if no placeholders were available.
     * @since 1.5.0
     */
    @NonNull
    public abstract String replacePlaceholders(@NonNull Player player, @NonNull String string);
}
