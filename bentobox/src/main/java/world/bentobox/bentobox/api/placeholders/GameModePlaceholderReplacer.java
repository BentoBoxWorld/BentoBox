package world.bentobox.bentobox.api.placeholders;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 *
 * @since 1.5.0
 * @author Poslovitch
 */
@FunctionalInterface
public interface GameModePlaceholderReplacer {

    /**
     * @param addon the GameModeAddon that registered the placeholder, cannot be null.
     * @param user the User to which the placeholder will be shown, can be null.
     * @param island the Island of the User, can be null.
     * @return the String containing the requested value or an empty String.
     */
    @NonNull
    String onReplace(@NonNull GameModeAddon addon, @Nullable User user, @Nullable Island island);
}
