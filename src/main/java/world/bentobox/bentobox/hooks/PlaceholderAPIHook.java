package world.bentobox.bentobox.hooks;

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

    public void registerBentoBoxPlaceholder(String placeholder, PlaceholderReplacer replacer) {
        bentoboxExpansion.registerPlaceholder(placeholder, replacer);
    }

    public void registerAddonPlaceholder(Addon addon, String placeholder, PlaceholderReplacer replacer) {
        // If addon is null, then register the placeholder in BentoBox's expansion.
        if (addon == null) {
            registerBentoBoxPlaceholder(placeholder, replacer);
        }

        // Check if the addon expansion does not exist
        if (!addonsExpansions.containsKey(addon)) {
            AddonPlaceholderExpansion addonPlaceholderExpansion = new AddonPlaceholderExpansion(addon);
            addonPlaceholderExpansion.register();
            addonsExpansions.put(addon, addonPlaceholderExpansion);
        }

        addonsExpansions.get(addon).registerPlaceholder(placeholder, replacer);
    }
}
