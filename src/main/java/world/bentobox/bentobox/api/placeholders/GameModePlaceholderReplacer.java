package world.bentobox.bentobox.api.placeholders;

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

    String onReplace(GameModeAddon addon, User user, Island island);
}
