package world.bentobox.bentobox.api.placeholders.placeholderapi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
abstract class BasicPlaceholderExpansion extends PlaceholderExpansion {
    @NonNull
    private final Map<@NonNull String, @NonNull PlaceholderReplacer> placeholders;

    BasicPlaceholderExpansion() {
        super();
        this.placeholders = new HashMap<>();
    }

    @Override
    public @NonNull String getIdentifier() {
        return getName().toLowerCase(Locale.ENGLISH);
    }

    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        placeholders.putIfAbsent(placeholder, replacer);
    }

    /**
     * Unregisters a placeholder from the expansion.
     * @param placeholder the placeholder to unregister.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
        placeholders.remove(placeholder);
    }

    @Override
    public String onPlaceholderRequest(@Nullable Player p, @NonNull String placeholder) {
        if (placeholders.containsKey(placeholder)) {
            return placeholders.get(placeholder).onReplace(p != null ? User.getInstance(p) : null);
        }
        return null;
    }

    /**
     * Checks if a placeholder with this name is already registered
     * @param placeholder - name of placeholder
     * @return <tt>true</tt> if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull String placeholder) {
        return placeholders.containsKey(placeholder);
    }

    @Override
    public boolean persist() {
        return true;
    }
}