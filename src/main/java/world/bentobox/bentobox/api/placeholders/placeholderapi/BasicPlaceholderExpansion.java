package world.bentobox.bentobox.api.placeholders.placeholderapi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
abstract class BasicPlaceholderExpansion extends PlaceholderExpansion {

    private record PlaceholderEntry(@NonNull PlaceholderReplacer replacer, @Nullable String description) {}

    @NonNull
    private final Map<@NonNull String, @NonNull PlaceholderEntry> placeholders;

    @NonNull
    private final Set<@NonNull String> disabledPlaceholders;

    BasicPlaceholderExpansion() {
        super();
        this.placeholders = new HashMap<>();
        this.disabledPlaceholders = new HashSet<>();
    }

    @Override
    public @NonNull String getIdentifier() {
        return getName().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Registers a placeholder with no description.
     * @param placeholder the placeholder identifier, not null.
     * @param replacer the replacer, not null.
     */
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        placeholders.putIfAbsent(placeholder, new PlaceholderEntry(replacer, null));
    }

    /**
     * Registers a placeholder with an optional plain-English description.
     * <p>
     * The description is a plain English string — <strong>not</strong> a locale key — that
     * briefly explains what the placeholder returns. It is displayed in the Placeholder GUI
     * and included in the output of {@code /bbox dump-placeholders}.
     * </p>
     * @param placeholder the placeholder identifier, not null.
     * @param description a short English description of what the placeholder returns, or null.
     * @param replacer the replacer, not null.
     * @since 3.2.0
     */
    public void registerPlaceholder(@NonNull String placeholder, @Nullable String description,
            @NonNull PlaceholderReplacer replacer) {
        placeholders.putIfAbsent(placeholder, new PlaceholderEntry(replacer, description));
    }

    /**
     * Unregisters a placeholder from the expansion.
     * @param placeholder the placeholder to unregister.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
        placeholders.remove(placeholder);
        disabledPlaceholders.remove(placeholder);
    }

    @Override
    public String onPlaceholderRequest(@Nullable Player p, @NonNull String placeholder) {
        PlaceholderEntry entry = placeholders.get(placeholder);
        if (entry == null) {
            return null;
        }
        if (disabledPlaceholders.contains(placeholder)) {
            return "";
        }
        return entry.replacer().onReplace(p != null ? User.getInstance(p) : null);
    }

    /**
     * Checks if a placeholder with this name is already registered.
     * @param placeholder - name of placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull String placeholder) {
        return placeholders.containsKey(placeholder);
    }

    /**
     * Returns an unmodifiable set of all registered placeholder identifiers.
     * @return set of placeholder identifiers, never null.
     * @since 3.2.0
     */
    @NonNull
    public Set<@NonNull String> getRegisteredPlaceholders() {
        return Set.copyOf(placeholders.keySet());
    }

    /**
     * Returns the description for the given placeholder, if one was provided at registration time.
     * The description is a plain English string, not a locale key.
     * @param placeholder the placeholder identifier.
     * @return an Optional containing the description, or empty if none was provided.
     * @since 3.2.0
     */
    @NonNull
    public Optional<String> getDescription(@NonNull String placeholder) {
        PlaceholderEntry entry = placeholders.get(placeholder);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.description());
    }

    /**
     * Sets whether a placeholder is currently enabled.
     * <p>
     * Disabled placeholders return an empty string instead of their resolved value.
     * This state is <em>not</em> persisted and will reset on server restart.
     * </p>
     * @param placeholder the placeholder identifier, not null.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @since 3.2.0
     */
    public void setEnabled(@NonNull String placeholder, boolean enabled) {
        if (enabled) {
            disabledPlaceholders.remove(placeholder);
        } else {
            disabledPlaceholders.add(placeholder);
        }
    }

    /**
     * Returns whether the given placeholder is currently enabled.
     * @param placeholder the placeholder identifier.
     * @return {@code true} if the placeholder is enabled (returns its value normally).
     * @since 3.2.0
     */
    public boolean isEnabled(@NonNull String placeholder) {
        return !disabledPlaceholders.contains(placeholder);
    }

    @Override
    public boolean persist() {
        return true;
    }
}
