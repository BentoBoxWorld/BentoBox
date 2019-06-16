package world.bentobox.bentobox.hooks.placeholders;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import me.clip.placeholderapi.PlaceholderAPI;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        return addonsExpansions.containsKey(addon) && addonsExpansions.get(addon).isPlaceholder(placeholder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String replacePlaceholders(@NonNull Player player, @NonNull String string) {
        return PlaceholderAPI.setPlaceholders(player, string);
    }
}
