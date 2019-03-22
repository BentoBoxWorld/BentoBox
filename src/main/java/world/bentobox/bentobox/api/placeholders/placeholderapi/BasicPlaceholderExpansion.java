package world.bentobox.bentobox.api.placeholders.placeholderapi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
abstract class BasicPlaceholderExpansion extends PlaceholderExpansion {
    @NonNull
    private Map<@NonNull String, @NonNull PlaceholderReplacer> placeholders;

    BasicPlaceholderExpansion() {
        this.placeholders = new HashMap<>();
    }

    @Override
    public String getIdentifier() {
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
    public String onPlaceholderRequest(Player p, String placeholder) {
        User user = User.getInstance(p);

        if (placeholders.containsKey(placeholder)) {
            return placeholders.get(placeholder).onReplace(user);
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
}