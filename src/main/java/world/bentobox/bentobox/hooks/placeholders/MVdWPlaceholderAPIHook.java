package world.bentobox.bentobox.hooks.placeholders;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

/**
 * Provides interfacing needed to register and get placeholders from MVdWPlaceholderAPI.
 * @author Poslovitch
 * @since 1.5.0
 */
public class MVdWPlaceholderAPIHook extends PlaceholderHook {

    public MVdWPlaceholderAPIHook() {
        super("MVdWPlaceholderAPI");
    }

    @Override
    public boolean hook() {
        return true; // There are no special checks to run when hooking into MVdWPlaceholderAPI.
    }

    @Override
    public String getFailureCause() {
        return "the version of MVdWPlaceholderAPI you're using is incompatible with this hook";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        PlaceholderAPI.registerPlaceholder(BentoBox.getInstance(), "bentobox_" + placeholder,
                event -> replacer.onReplace(User.getInstance(event.getPlayer())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerPlaceholder(@NonNull Addon addon, @NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        PlaceholderAPI.registerPlaceholder(BentoBox.getInstance(), addon.getDescription().getName().toLowerCase() + "_" + placeholder,
                event -> replacer.onReplace(User.getInstance(event.getPlayer())));
    }

    /**
     * {@inheritDoc}
     * <b>This is not supported by MVdWPlaceholderAPI. #HighQualityContent.</b>
     */
    @Override
    public void unregisterPlaceholder(@NonNull String placeholder) {
        // Do nothing: not supported by MVdW. #HighQualityContent
    }

    /**
     * {@inheritDoc}
     * <b>This is not supported by MVdWPlaceholderAPI. #HighQualityContent.</b>
     */
    @Override
    public void unregisterPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        // Do nothing: not supported by MVdW. #HighQualityContent
    }

    /**
     * {@inheritDoc}
     * <b>This is not supported by MVdWPlaceholderAPI. #HighQualityContent.</b>
     * As a result, this will <b>always return {@code false}</b>.
     */
    @Override
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        return false; // Do nothing: not supported by MVdW. #HighQualityContent
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String replacePlaceholders(@NonNull Player player, @NonNull String string) {
        return PlaceholderAPI.replacePlaceholders(player, string);
    }
}
