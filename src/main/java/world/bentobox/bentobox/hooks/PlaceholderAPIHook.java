package world.bentobox.bentobox.hooks;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.placeholders.placeholderapi.AddonPlaceholderExpansion;
import world.bentobox.bentobox.api.placeholders.placeholderapi.BentoBoxPlaceholderExpansion;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides implementations and interfacing needed to register and get placeholders from PlaceholderAPI.
 *
 * @author Poslovitch
 */
public class PlaceholderAPIHook extends Hook {

    private BentoBoxPlaceholderExpansion bentoboxExpansion;
    private Map<Addon, AddonPlaceholderExpansion> addonsExpansions;

    public PlaceholderAPIHook() {
        super("PlaceholderAPI");
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

    /**
     * @deprecated As of 1.4.0, renamed to {@link #registerPlaceholder(String, PlaceholderReplacer)}.
     */
    @Deprecated
    public void registerBentoBoxPlaceholder(String placeholder, PlaceholderReplacer replacer) {
        registerPlaceholder(placeholder, replacer);
    }

    /**
     * Registers this placeholder into BentoBox's PlaceholderAPI expansion.
     * @param placeholder the placeholder to register, not null
     * @param replacer its replacement, not null
     * @since 1.4.0
     */
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        bentoboxExpansion.registerPlaceholder(placeholder, replacer);
    }

    /**
     * Registers this placeholder into this addon's PlaceholderAPI expansion.
     * It will register the expansion if it previously did not exist.
     * @param addon the addon, not null
     * @param placeholder the placeholder to register, not null
     * @param replacer its replacement, not null
     * @since 1.4.0
     */
    public void registerPlaceholder(@NonNull Addon addon, @NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        // Check if the addon expansion does not exist
        if (!addonsExpansions.containsKey(addon)) {
            AddonPlaceholderExpansion addonPlaceholderExpansion = new AddonPlaceholderExpansion(addon);
            addonPlaceholderExpansion.register();
            addonsExpansions.put(addon, addonPlaceholderExpansion);
        }

        addonsExpansions.get(addon).registerPlaceholder(placeholder, replacer);
    }

    /**
     * Unregisters this placeholder from the BentoBox PlaceholderAPI expansion.
     * @param placeholder the placeholder to unregister, not null
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
        bentoboxExpansion.unregisterPlaceholder(placeholder);
    }

    /**
     * Unregister this placeholder from this addon's PlaceholderAPI expansion.
     * @param addon the addon, not null
     * @param placeholder the placeholder to unregister, not null
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        if (addonsExpansions.containsKey(addon)) {
            addonsExpansions.get(addon).unregisterPlaceholder(placeholder);
        }
    }

    /**
     * @deprecated As of 1.4.0, renamed to {@link #registerPlaceholder(Addon, String, PlaceholderReplacer)}.
     */
    @Deprecated
    public void registerAddonPlaceholder(Addon addon, String placeholder, PlaceholderReplacer replacer) {
        registerPlaceholder(addon, placeholder, replacer);
    }
    
    /**
     * Checks if a placeholder with this name is already registered
     * @param addon the addon, not null
     * @param placeholder - name of placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
    	return addonsExpansions.containsKey(addon) && addonsExpansions.get(addon).isPlaceholder(placeholder);
    }
}
