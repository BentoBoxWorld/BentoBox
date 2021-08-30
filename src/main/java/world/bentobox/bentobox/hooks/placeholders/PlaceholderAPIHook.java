package world.bentobox.bentobox.hooks.placeholders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

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
    private final Set<String> bentoBoxPlaceholders;
    private final Map<Addon, Set<String>> addonPlaceholders;


    public PlaceholderAPIHook() {
        super("PlaceholderAPI");
        this.addonsExpansions = new HashMap<>();
        this.bentoBoxPlaceholders = new HashSet<>();
        this.addonPlaceholders = new HashMap<>();
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
        this.bentoBoxPlaceholders.add(placeholder);
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
            this.addonPlaceholders.computeIfAbsent(addon, k -> new HashSet<>()).add(placeholder);
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
     *
     */
    @Override
    @NonNull
    public String replacePlaceholders(@NonNull Player player, @NonNull String string) {
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

    /**
     * Used for unit testing only
     * @param bentoboxExpansion the bentoboxExpansion to set
     */
    protected void setBentoboxExpansion(BentoBoxPlaceholderExpansion bentoboxExpansion) {
        this.bentoboxExpansion = bentoboxExpansion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        this.bentoBoxPlaceholders.forEach(this::unregisterPlaceholder);
        this.addonPlaceholders.forEach((addon,list) -> list.forEach(placeholder -> this.unregisterPlaceholder(addon, placeholder)));
    }
}
