package world.bentobox.bentobox.managers;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.lists.GameModePlaceholder;

/**
 * Manages placeholder integration.
 *
 * @author Poslovitch
 */
public class PlaceholdersManager {

    private static final int MAX_TEAM_MEMBER_PLACEHOLDERS = 50;
    private final BentoBox plugin;

    public PlaceholdersManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers this placeholder on the behalf of BentoBox.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with {@code "bentobox_"} by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will be this placeholder's replacement.
     */
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        // Register it in PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(placeholder, replacer));
    }

    /**
     * Registers this placeholder on the behalf of the specified addon.
     * @param addon the addon to register this placeholder on its behalf.
     *              If null, the placeholder will be registered using {@link #registerPlaceholder(String, PlaceholderReplacer)}.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with the addon's name by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will replace the placeholder.
     */
    public void registerPlaceholder(@Nullable Addon addon, @NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, replacer);
            return;
        }
        // Register it in PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(addon, placeholder, replacer));
    }

    /**
     * Registers default placeholders for this gamemode addon.
     * @param addon the gamemode addon to register the default placeholders too.
     * @since 1.5.0
     */
    public void registerDefaultPlaceholders(@NonNull GameModeAddon addon) {
        Arrays.stream(GameModePlaceholder.values())
        .filter(placeholder -> !isPlaceholder(addon, placeholder.getPlaceholder()))
        .forEach(placeholder -> registerPlaceholder(addon, placeholder.getPlaceholder(), new DefaultPlaceholder(addon, placeholder)));
        // Register team member placeholders
        registerTeamMemberPlaceholders(addon);
    }

    private void registerTeamMemberPlaceholders(@NonNull GameModeAddon addon) {
        for (int i = 1; i <= MAX_TEAM_MEMBER_PLACEHOLDERS; i++) {
            final int count = i;
            // Names
            registerPlaceholder(addon, "island_member_name_" + i, user -> {
                if (user != null) {
                    Island island = plugin.getIslands().getIsland(addon.getOverWorld(), user);
                    int j = 1;
                    for (UUID uuid : island.getMemberSet(RanksManager.MEMBER_RANK)) {
                        if (j++ == count) {
                            return plugin.getPlayers().getName(uuid);
                        }
                    }
                }
                return "";
            });
            // Register ranks
            registerPlaceholder(addon, "island_member_rank_" + i, user -> {
                if (user != null) {
                    Island island = plugin.getIslands().getIsland(addon.getOverWorld(), user);
                    int j = 1;
                    for (UUID uuid : island.getMemberSet(RanksManager.MEMBER_RANK)) {
                        if (j++ == count) {
                            return user.getTranslationOrNothing(RanksManager.getInstance().getRank(island.getRank(uuid)));
                        }
                    }
                }
                return "";
            });
            // Banned
            registerPlaceholder(addon, "island_banned_name_" + i, user -> {
                if (user != null) {
                    Island island = plugin.getIslands().getIsland(addon.getOverWorld(), user);
                    int j = 1;
                    for (UUID uuid : island.getBanned()) {
                        if (j++ == count) {
                            return plugin.getPlayers().getName(uuid);
                        }
                    }
                }
                return "";
            });
            // Visited Island
            registerPlaceholder(addon, "visited_island_member_name_" + i, user -> {
                if (user != null) {
                    return plugin.getIslands().getIslandAt(user.getLocation())
                            .filter(island -> addon.inWorld(island.getCenter()))
                            .map(island -> {
                                int j = 1;
                                for (UUID uuid : island.getMemberSet(RanksManager.MEMBER_RANK)) {
                                    if (j++ == count) {
                                        return plugin.getPlayers().getName(uuid);
                                    }
                                }
                                return "";
                            }).orElse("");

                }
                return "";
            });
            registerPlaceholder(addon, "visited_island_member_rank_" + i, user -> {
                if (user != null) {
                    return plugin.getIslands().getIslandAt(user.getLocation())
                            .filter(island -> addon.inWorld(island.getCenter()))
                            .map(island -> {
                                int j = 1;
                                for (UUID uuid : island.getMemberSet(RanksManager.MEMBER_RANK)) {
                                    if (j++ == count) {
                                        return user.getTranslationOrNothing(RanksManager.getInstance().getRank(island.getRank(uuid)));
                                    }
                                }
                                return "";
                            }).orElse("");

                }
                return "";
            });
            registerPlaceholder(addon, "visited_island_banned_name_" + i, user -> {
                if (user != null) {
                    return plugin.getIslands().getIslandAt(user.getLocation())
                            .filter(island -> addon.inWorld(island.getCenter()))
                            .map(island -> {
                                int j = 1;
                                for (UUID uuid : island.getBanned()) {
                                    if (j++ == count) {
                                        return plugin.getPlayers().getName(uuid);
                                    }
                                }
                                return "";
                            }).orElse("");

                }
                return "";
            });
        }
    }

    /**
     * Unregisters this placeholder on the behalf of BentoBox.
     * Note that if the placeholder you are trying to unregister has been registered by an addon, you should use {@link #unregisterPlaceholder(Addon, String)} instead.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
        // Unregister it from PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.unregisterPlaceholder(placeholder));
    }

    /**
     * Unregisters this placeholder on the behalf of the specified addon.
     * @param addon the addon that originally registered this placeholder.
     *              If null, this placeholder will be unregistered using {@link #unregisterPlaceholder(String)}.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@Nullable Addon addon, @NonNull String placeholder) {
        if (addon == null) {
            unregisterPlaceholder(placeholder);
            return;
        }
        // Unregister it from PlaceholderAPI
        getPlaceholderAPIHook().ifPresent(hook -> hook.unregisterPlaceholder(addon, placeholder));
    }

    /**
     * Returns an Optional containing the PlaceholderAPIHook instance, or an empty Optional otherwise.
     * @return Optional containing the PlaceholderAPIHook instance or an empty Optional otherwise.
     * @since 1.4.0
     */
    @NonNull
    private Optional<PlaceholderAPIHook> getPlaceholderAPIHook() {
        return plugin.getHooks().getHook("PlaceholderAPI").map(PlaceholderAPIHook.class::cast);
    }

    /**
     * Checks if a placeholder with this name is already registered
     * @param addon the addon, not null
     * @param placeholder - name of placeholder
     * @return {@code true} if a placeholder with this name is already registered
     * @since 1.4.0
     */
    public boolean isPlaceholder(@NonNull Addon addon, @NonNull String placeholder) {
        return getPlaceholderAPIHook().map(h -> h.isPlaceholder(addon, placeholder)).orElse(false);
    }

    /**
     * Replaces the placeholders in this String and returns it.
     * @param player the Player to get the placeholders for or null for non-player-specific placeholders
     * @param string the String to replace the placeholders in.
     * @return the String with placeholders replaced, or the identical String if no placeholders were available.
     * @since 1.5.0
     */
    public String replacePlaceholders(@Nullable Player player, @NonNull String string) {
        return getPlaceholderAPIHook().map(papi -> papi.replacePlaceholders(player, string)).orElse(string);
    }

    /**
     * Unregisters all the placeholders.
     * @since 1.15.0
     */
    public void unregisterAll() {
        getPlaceholderAPIHook().ifPresent(PlaceholderAPIHook::unregisterAll);

    }

    /**
     * Default placeholder
     *
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
}
