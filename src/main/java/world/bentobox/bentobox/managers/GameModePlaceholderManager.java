package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.GameModePlaceholder;

/**
 * Registers default placeholders for all GameModes. Will not overwrite any that the gamemode addon itself implements.
 * @author tastybento
 * @since 1.4.0
 * @deprecated As of 1.5.0, for removal.
 */
@Deprecated
public class GameModePlaceholderManager {

    private BentoBox plugin;

    public GameModePlaceholderManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * @since 1.4.0
     * @deprecated As of 1.5.0, for removal. Use {@link PlaceholdersManager#registerDefaultPlaceholders(GameModeAddon)} instead.
     */
    @Deprecated
    public void registerGameModePlaceholders(@NonNull GameModeAddon addon) {
        plugin.getPlaceholdersManager().registerDefaultPlaceholders(addon);
    }
}

/**
 * @author tastybento
 * @since 1.4.0
 */
class DefaultPlaceholder implements PlaceholderReplacer {
    private final GameModeAddon addon;
    private final GameModePlaceholder type;
    public DefaultPlaceholder(GameModeAddon addon, GameModePlaceholder type) {
        this.addon = addon;
        this.type = type;
    }
    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
     */
    @NonNull
    @Override
    public String onReplace(@Nullable User user) {
        if (user == null) {
            return "";
        }
        Island island = addon.getIslands().getIsland(addon.getOverWorld(), user);

        return type.getReplacer().onReplace(addon, user, island);
    }
}



