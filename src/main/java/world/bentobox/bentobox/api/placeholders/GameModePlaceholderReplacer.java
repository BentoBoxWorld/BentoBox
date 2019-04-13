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

    @NonNull
    String onReplace(@NonNull GameModeAddon addon, @Nullable User user, @Nullable Island island);
}
