package world.bentobox.bentobox.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

import java.util.HashMap;
import java.util.Locale;
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
        this.bentoboxExpansion = new BentoBoxPlaceholderExpansion(BentoBox.getInstance());
        this.addonsExpansions = new HashMap<>();
    }

    @Override
    public boolean hook() {
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
        // Check if the addon expansion does not exist
        if (!addonsExpansions.containsKey(addon)) {
            AddonPlaceholderExpansion addonPlaceholderExpansion = new AddonPlaceholderExpansion(addon);
            addonPlaceholderExpansion.register();
            addonsExpansions.put(addon, addonPlaceholderExpansion);
        }

        addonsExpansions.get(addon).registerPlaceholder(placeholder, replacer);
    }

    abstract class BasicPlaceholderExpansion extends PlaceholderExpansion {
        private Map<String, PlaceholderReplacer> placeholders;

        BasicPlaceholderExpansion() {
            this.placeholders = new HashMap<>();
        }

        @Override
        public String getIdentifier() {
            return getName().toLowerCase(Locale.ENGLISH);
        }

        void registerPlaceholder(String placeholder, PlaceholderReplacer replacer) {
            placeholders.putIfAbsent(placeholder, replacer);
        }

        @Override
        public String onPlaceholderRequest(Player p, String placeholder) {
            User user = User.getInstance(p);

            if (placeholders.containsKey(placeholder)) {
                return placeholders.get(placeholder).onReplace(user);
            }
            return null;
        }
    }

    class BentoBoxPlaceholderExpansion extends BasicPlaceholderExpansion {
        private BentoBox plugin;

        BentoBoxPlaceholderExpansion(BentoBox plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getName() {
            return plugin.getName();
        }

        @Override
        public String getAuthor() {
            return "Tastybento and Poslovitch";
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }
    }

    class AddonPlaceholderExpansion extends BasicPlaceholderExpansion {
        private Addon addon;

        AddonPlaceholderExpansion(Addon addon) {
            this.addon = addon;
        }

        @Override
        public String getName() {
            return addon.getDescription().getName();
        }

        @Override
        public String getAuthor() {
            return addon.getDescription().getAuthors().get(0);
        }

        @Override
        public String getVersion() {
            return addon.getDescription().getVersion();
        }
    }
}
