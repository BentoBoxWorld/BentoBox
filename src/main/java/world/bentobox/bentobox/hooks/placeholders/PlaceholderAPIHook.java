package world.bentobox.bentobox.hooks.placeholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.placeholders.placeholderapi.AddonPlaceholderExpansion;
import world.bentobox.bentobox.api.placeholders.placeholderapi.BentoBoxPlaceholderExpansion;

/**
 * Provides implementations and interfacing needed to register and get placeholders from PlaceholderAPI.
 *
 * @author Poslovitch
 */
public class PlaceholderAPIHook extends PlaceholderHook {

    private BentoBoxPlaceholderExpansion bentoboxExpansion;
    private final Map<Addon, AddonPlaceholderExpansion> addonsExpansions;


    public PlaceholderAPIHook() {
        super();
        this.addonsExpansions = new HashMap<>();
    }

    @Override
    public boolean hook() {
        try {
            this.bentoboxExpansion = new BentoBoxPlaceholderExpansion(BentoBox.getInstance());
        } catch (Exception | NoClassDefFoundError | NoSuchMethodError e) {
            return false;
        }

        return bentoboxExpansion.canRegister() && bentoboxExpansion.register();
    }

    @Override
    public String getFailureCause() {
        return "could not register BentoBox's expansion";
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        bentoboxExpansion.registerPlaceholder(placeholder, replacer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull String placeholder, @Nullable String description,
            @NonNull PlaceholderReplacer replacer) {
        bentoboxExpansion.registerPlaceholder(placeholder, description, replacer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull Addon addon, @NonNull String placeholder,
            @NonNull PlaceholderReplacer replacer) {
        registerPlaceholder(addon, placeholder, null, replacer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull Addon addon, @NonNull String placeholder, @Nullable String description,
            @NonNull PlaceholderReplacer replacer) {
        // Create the addon expansion if it does not exist yet
        addonsExpansions.computeIfAbsent(addon, k -> {
            AddonPlaceholderExpansion expansion = new AddonPlaceholderExpansion(addon);
            expansion.register();
            return expansion;
        });
        addonsExpansions.get(addon).registerPlaceholder(placeholder, description, replacer);
    }

    // -------------------------------------------------------------------------
    // Unregistration
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterPlaceholder(@NonNull String placeholder) {
        bentoboxExpansion.unregisterPlaceholder(placeholder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        if (addonsExpansions.containsKey(addon)) {
            addonsExpansions.get(addon).unregisterPlaceholder(placeholder);
        }
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        return addonsExpansions.containsKey(addon) && addonsExpansions.get(addon).isPlaceholder(placeholder);
    }

    /**
     * Returns the set of BentoBox-core placeholder identifiers (registered without an addon).
     * @return unmodifiable set of placeholder identifiers, never null.
     * @since 3.2.0
     */
    @NonNull
    public Set<String> getBentoBoxPlaceholders() {
        return bentoboxExpansion.getRegisteredPlaceholders();
    }

    /**
     * Returns the set of placeholder identifiers registered by the given addon.
     * @param addon the addon, not null.
     * @return unmodifiable set of placeholder identifiers, or empty set if the addon has none.
     * @since 3.2.0
     */
    @NonNull
    public Set<String> getAddonPlaceholders(@NonNull Addon addon) {
        AddonPlaceholderExpansion exp = addonsExpansions.get(addon);
        return exp == null ? Set.of() : exp.getRegisteredPlaceholders();
    }

    /**
     * Returns all addons that have at least one registered placeholder expansion.
     * @return unmodifiable set of addons, never null.
     * @since 3.2.0
     */
    @NonNull
    public Set<Addon> getAddonsWithPlaceholders() {
        return Set.copyOf(addonsExpansions.keySet());
    }

    /**
     * Returns the description for a BentoBox-core placeholder.
     * @param placeholder the placeholder identifier.
     * @return Optional containing the description, or empty.
     * @since 3.2.0
     */
    @NonNull
    public Optional<String> getDescription(@NonNull String placeholder) {
        return bentoboxExpansion.getDescription(placeholder);
    }

    /**
     * Returns the description for an addon placeholder.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier.
     * @return Optional containing the description, or empty.
     * @since 3.2.0
     */
    @NonNull
    public Optional<String> getDescription(@NonNull Addon addon, @NonNull String placeholder) {
        AddonPlaceholderExpansion exp = addonsExpansions.get(addon);
        return exp == null ? Optional.empty() : exp.getDescription(placeholder);
    }

    // -------------------------------------------------------------------------
    // Enable / disable
    // -------------------------------------------------------------------------

    /**
     * Sets whether a BentoBox-core placeholder is enabled.
     * Disabled placeholders return an empty string instead of their resolved value.
     * This state is not persisted and resets on server restart.
     * @param placeholder the placeholder identifier, not null.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @since 3.2.0
     */
    public void setEnabled(@NonNull String placeholder, boolean enabled) {
        bentoboxExpansion.setEnabled(placeholder, enabled);
    }

    /**
     * Sets whether an addon placeholder is enabled.
     * Disabled placeholders return an empty string instead of their resolved value.
     * This state is not persisted and resets on server restart.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier, not null.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @since 3.2.0
     */
    public void setEnabled(@NonNull Addon addon, @NonNull String placeholder, boolean enabled) {
        AddonPlaceholderExpansion exp = addonsExpansions.get(addon);
        if (exp != null) {
            exp.setEnabled(placeholder, enabled);
        }
    }

    /**
     * Returns whether a BentoBox-core placeholder is currently enabled.
     * @param placeholder the placeholder identifier.
     * @return {@code true} if enabled.
     * @since 3.2.0
     */
    public boolean isEnabled(@NonNull String placeholder) {
        return bentoboxExpansion.isEnabled(placeholder);
    }

    /**
     * Returns whether an addon placeholder is currently enabled.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier.
     * @return {@code true} if enabled (or if the addon has no expansion registered).
     * @since 3.2.0
     */
    public boolean isEnabled(@NonNull Addon addon, @NonNull String placeholder) {
        AddonPlaceholderExpansion exp = addonsExpansions.get(addon);
        return exp == null || exp.isEnabled(placeholder);
    }

    // -------------------------------------------------------------------------
    // Replacement
    // -------------------------------------------------------------------------

    /**
     *
     */
    @Override
    @NonNull
    public String replacePlaceholders(@Nullable Player player, @NonNull String string) {
        if (player == null) {
            return PlaceholderAPI.setPlaceholders(player, removeGMPlaceholder(string));
        }
        // Transform [gamemode] in string to the game mode description name, or remove it for the default replacement
        String newString = BentoBox.getInstance().getIWM().getAddon(player.getWorld()).map(gm ->
        string.replace(TextVariables.GAMEMODE, gm.getDescription().getName().toLowerCase())
                ).orElseGet(() -> removeGMPlaceholder(string));
        return PlaceholderAPI.setPlaceholders(player, newString);
    }

    private String removeGMPlaceholder(@NonNull String string) {
        String newString = string;
        // Get placeholders - TODO: my regex moh=jo isn't good enough to grab only placeholders with [gamemode] in yet!
        Matcher m = Pattern.compile("(%)(.*?)(%)").matcher(string);
        while (m.find()) {
            String ph = m.group();
            if (ph.contains(TextVariables.GAMEMODE)) newString = newString.replace(ph,"");
        }
        return newString;
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    /**
     * Used for unit testing only
     * @param bentoboxExpansion the bentoboxExpansion to set
     */
    protected void setBentoboxExpansion(BentoBoxPlaceholderExpansion bentoboxExpansion) {
        this.bentoboxExpansion = bentoboxExpansion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only clears the BentoBox-core expansion. Placeholders registered by addons
     * via {@link #registerPlaceholder(Addon, String, PlaceholderReplacer)} are
     * intentionally preserved, because callers like {@code /bbox reload} do not
     * re-invoke addons and would otherwise leave addon placeholders in a stale,
     * empty state (see #2930). To clear a specific addon's placeholders, use
     * {@link #unregisterAll(Addon)}.
     */
    @Override
    public void unregisterAll() {
        bentoboxExpansion.getRegisteredPlaceholders().forEach(this::unregisterPlaceholder);
    }

    /**
     * Unregisters all placeholders previously registered for the given addon.
     * The expansion object stays registered with PlaceholderAPI but its
     * internal placeholder map is emptied.
     * @param addon the addon whose placeholders should be cleared, not null.
     * @since 3.4.0
     */
    public void unregisterAll(@NonNull Addon addon) {
        AddonPlaceholderExpansion exp = addonsExpansions.get(addon);
        if (exp != null) {
            exp.getRegisteredPlaceholders().forEach(exp::unregisterPlaceholder);
        }
    }
}
