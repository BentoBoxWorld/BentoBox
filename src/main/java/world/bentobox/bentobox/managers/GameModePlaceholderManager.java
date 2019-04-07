package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.GameModePlaceholders;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registers default placeholders for all GameModes. Will not overwrite any that the gamemode addon itself implements.
 * @author tastybento
 * @since 1.4.0
 */
public class GameModePlaceholderManager {

    private BentoBox plugin;

    public GameModePlaceholderManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    public void registerGameModePlaceholders(GameModeAddon addon) {
        String prefix = addon.getDescription().getName().toLowerCase();
        Map<GameModePlaceholders, String> placeholders = new EnumMap<>(GameModePlaceholders.class);
        Arrays.stream(GameModePlaceholders.values()).forEach(placeholder -> placeholders.put(placeholder, prefix + "-" + placeholder.getPlaceholder()));

        // Register placeholders only if they have not already been registered by the addon itself
        placeholders.entrySet().stream().filter(en -> !plugin.getPlaceholdersManager().isPlaceholder(addon, en.getValue()))
                .forEach(en -> plugin.getPlaceholdersManager().registerPlaceholder(en.getValue(), new DefaultPlaceholder(addon, en.getKey())));
    }
}

/**
 * @author tastybento
 * @since 1.4.0
 */
class DefaultPlaceholder implements PlaceholderReplacer {
    private final GameModeAddon addon;
    private final GameModePlaceholders type;
    public DefaultPlaceholder(GameModeAddon addon, GameModePlaceholders type) {
        this.addon = addon;
        this.type = type;
    }
    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
     */
    @Override
    public String onReplace(User user) {
        if (user == null) {
            return "";
        }
        Island island = addon.getIslands().getIsland(addon.getOverWorld(), user);
        if (island == null) {
            return "";
        }

        return type.getReplacer().onReplace(addon, user, island);
    }
}



