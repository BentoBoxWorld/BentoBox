package world.bentobox.bentobox.managers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
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

    // -------------------------------------------------------------------------
    // Registration — BentoBox core
    // -------------------------------------------------------------------------

    /**
     * Registers this placeholder on the behalf of BentoBox.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with {@code "bentobox_"} by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will be this placeholder's replacement.
     */
    public void registerPlaceholder(@NonNull String placeholder, @NonNull PlaceholderReplacer replacer) {
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(placeholder, replacer));
    }

    /**
     * Registers this placeholder on the behalf of BentoBox with a plain-English description.
     * <p>
     * The description is a plain English string — <strong>not</strong> a locale key — that
     * briefly explains what the placeholder returns. It is displayed in the Placeholder GUI
     * and included in the output of {@code /bbox dump-placeholders}.
     * </p>
     * @param placeholder the placeholder to register, not null.
     * @param description a short English description of what the placeholder returns, or null.
     * @param replacer the expression that will return a {@code String} when executed.
     * @since 3.2.0
     */
    public void registerPlaceholder(@NonNull String placeholder, @Nullable String description,
            @NonNull PlaceholderReplacer replacer) {
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(placeholder, description, replacer));
    }

    // -------------------------------------------------------------------------
    // Registration — addon
    // -------------------------------------------------------------------------

    /**
     * Registers this placeholder on the behalf of the specified addon.
     * @param addon the addon to register this placeholder on its behalf.
     *              If null, the placeholder will be registered using {@link #registerPlaceholder(String, PlaceholderReplacer)}.
     * @param placeholder the placeholder to register, not null.
     *                    It will be appended with the addon's name by the placeholder plugin.
     * @param replacer the expression that will return a {@code String} when executed, which will replace the placeholder.
     */
    public void registerPlaceholder(@Nullable Addon addon, @NonNull String placeholder,
            @NonNull PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, replacer);
            return;
        }
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(addon, placeholder, replacer));
    }

    /**
     * Registers this placeholder on the behalf of the specified addon with a plain-English description.
     * <p>
     * The description is a plain English string — <strong>not</strong> a locale key — that
     * briefly explains what the placeholder returns. It is displayed in the Placeholder GUI
     * and included in the output of {@code /bbox dump-placeholders}.
     * </p>
     * <p>
     * Example usage in an addon:
     * </p>
     * <pre>{@code
     * plugin.getPlaceholdersManager().registerPlaceholder(
     *     this, "my_value", "The current value of my feature", user -> computeValue());
     * }</pre>
     * @param addon the addon to register this placeholder on its behalf.
     *              If null, falls back to {@link #registerPlaceholder(String, String, PlaceholderReplacer)}.
     * @param placeholder the placeholder to register, not null.
     * @param description a short English description of what the placeholder returns, or null.
     * @param replacer the expression that will return a {@code String} when executed.
     * @since 3.2.0
     */
    public void registerPlaceholder(@Nullable Addon addon, @NonNull String placeholder, @Nullable String description,
            @NonNull PlaceholderReplacer replacer) {
        if (addon == null) {
            registerPlaceholder(placeholder, description, replacer);
            return;
        }
        getPlaceholderAPIHook().ifPresent(hook -> hook.registerPlaceholder(addon, placeholder, description, replacer));
    }

    /**
     * Registers default placeholders for this gamemode addon.
     * @param addon the gamemode addon to register the default placeholders too.
     * @since 1.5.0
     */
    public void registerDefaultPlaceholders(@NonNull GameModeAddon addon) {
        Arrays.stream(GameModePlaceholder.values())
        .filter(placeholder -> !isPlaceholder(addon, placeholder.getPlaceholder()))
        .forEach(placeholder -> registerPlaceholder(addon, placeholder.getPlaceholder(),
                placeholder.getDescription(),
                new DefaultPlaceholder(addon, placeholder)));
        // Register team member placeholders
        registerTeamMemberPlaceholders(addon);
        // Register potential island names and member info
        registerOwnedIslandPlaceholders(addon);
        // Register flag placeholders for all currently registered flags
        registerFlagPlaceholders(addon);
    }

    private void registerOwnedIslandPlaceholders(@NonNull GameModeAddon addon) {
        int maxIslands = plugin.getIWM().getWorldSettings(addon.getOverWorld()).getConcurrentIslands();
        IntStream.range(0, maxIslands).forEach(i -> registerPlaceholder(addon, "island_name_" + (i + 1),
                "Name of the player's island #" + (i + 1),
                user -> {
            if (user == null)
                return "";

            AtomicInteger generatedCount = new AtomicInteger(1); // To increment within lambda
            return plugin.getIslands().getIslands(addon.getOverWorld(), user).stream().map(island -> {
                IslandName islandName = getIslandName(island, user, generatedCount.get());
                if (islandName.generated()) {
                    generatedCount.getAndIncrement(); // Increment if the name was generated
                    }
                return islandName.name;
            }).skip(i) // Skip to the island at index 'i'
                    .findFirst() // Take the first island after skipping, effectively the (i+1)th
                    .orElse(""); // Default to empty string if no island is found
        }));

        // Island_memberlist
        IntStream.range(0, maxIslands)
                .forEach(i -> registerPlaceholder(addon, "island_memberlist_" + (i + 1),
                        "Comma-separated member list for the player's island #" + (i + 1),
                        user -> user == null ? ""
                        : plugin.getIslands().getIslands(addon.getOverWorld(), user).stream().skip(i).findFirst()
                                .map(island -> island.getMemberSet().stream()
                                        .map(addon.getPlayers()::getName).collect(Collectors.joining(",")))
                                .orElse("")));
    }

    private record IslandName(String name, boolean generated) {
    }

    private IslandName getIslandName(Island island, User user, int index) {
        if (island.getName() != null && !island.getName().isBlank()) {
            // Name has been set
            return new IslandName(island.getName(), false);
        } else {
            // Name has not been set
            return new IslandName(user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME,
                    user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName()) + " " + index, true);
        }

    }

    private void registerTeamMemberPlaceholders(@NonNull GameModeAddon addon) {
        for (int i = 1; i <= MAX_TEAM_MEMBER_PLACEHOLDERS; i++) {
            final int index = i;
            // Own island: member name, rank, banned name
            registerPlaceholder(addon, "island_member_name_" + i,
                    "Name of island member #" + i + " (ranked member or above)",
                    user -> resolveOwnIsland(addon, user, island -> getNthName(island.getMemberSet(RanksManager.MEMBER_RANK), index)));
            registerPlaceholder(addon, "island_member_rank_" + i,
                    "Rank of island member #" + i,
                    user -> resolveOwnIsland(addon, user, island -> getNthRank(island, user, index)));
            registerPlaceholder(addon, "island_banned_name_" + i,
                    "Name of banned player #" + i,
                    user -> resolveOwnIsland(addon, user, island -> getNthName(island.getBanned(), index)));
            // Visited island: member name, rank, banned name
            registerPlaceholder(addon, "visited_island_member_name_" + i,
                    "Name of member #" + i + " of the island the player is standing on",
                    user -> resolveVisitedIsland(addon, user, island -> getNthName(island.getMemberSet(RanksManager.MEMBER_RANK), index)));
            registerPlaceholder(addon, "visited_island_member_rank_" + i,
                    "Rank of member #" + i + " of the island the player is standing on",
                    user -> resolveVisitedIsland(addon, user, island -> getNthRank(island, user, index)));
            registerPlaceholder(addon, "visited_island_banned_name_" + i,
                    "Name of banned player #" + i + " on the island the player is standing on",
                    user -> resolveVisitedIsland(addon, user, island -> getNthName(island.getBanned(), index)));
        }
        registerOnlineMemberCountPlaceholders(addon);
    }

    private void registerOnlineMemberCountPlaceholders(@NonNull GameModeAddon addon) {
        registerPlaceholder(addon, "island_online_members_count",
                "Number of island members currently online",
                user -> resolveOwnIsland(addon, user, this::countOnlineMembers));
        registerPlaceholder(addon, "visited_island_online_members_count",
                "Number of members currently online on the island the player is standing on",
                user -> resolveVisitedIsland(addon, user, this::countOnlineMembers));
    }

    private String countOnlineMembers(Island island) {
        return String.valueOf(island.getMemberSet(RanksManager.MEMBER_RANK).stream()
                .map(Bukkit::getOfflinePlayer).filter(OfflinePlayer::isOnline).count());
    }

    private String resolveOwnIsland(GameModeAddon addon, User user, Function<Island, String> resolver) {
        if (user == null) {
            return "";
        }
        Island island = plugin.getIslands().getIsland(addon.getOverWorld(), user);
        return island != null ? resolver.apply(island) : "";
    }

    private String resolveVisitedIsland(GameModeAddon addon, User user, Function<Island, String> resolver) {
        if (user == null) {
            return "";
        }
        return plugin.getIslands().getIslandAt(user.getLocation())
                .filter(island -> addon.inWorld(island.getCenter()))
                .map(resolver)
                .orElse("");
    }

    private String getNthName(Collection<UUID> uuids, int index) {
        int j = 1;
        for (UUID uuid : uuids) {
            if (j++ == index) {
                return plugin.getPlayers().getName(uuid);
            }
        }
        return "";
    }

    private String getNthRank(Island island, User user, int index) {
        int j = 1;
        for (UUID uuid : island.getMemberSet(RanksManager.MEMBER_RANK)) {
            if (j++ == index) {
                return user.getTranslationOrNothing(RanksManager.getInstance().getRank(island.getRank(uuid)));
            }
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Unregistration
    // -------------------------------------------------------------------------

    /**
     * Unregisters this placeholder on the behalf of BentoBox.
     * Note that if the placeholder you are trying to unregister has been registered by an addon, you should use {@link #unregisterPlaceholder(Addon, String)} instead.
     * @param placeholder the placeholder to unregister, not null.
     * @since 1.4.0
     */
    public void unregisterPlaceholder(@NonNull String placeholder) {
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
        getPlaceholderAPIHook().ifPresent(hook -> hook.unregisterPlaceholder(addon, placeholder));
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

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
     * Returns the set of BentoBox-core placeholder identifiers.
     * @return unmodifiable set of placeholder identifiers, or empty set if PlaceholderAPI is not hooked.
     * @since 3.2.0
     */
    @NonNull
    public Set<String> getRegisteredBentoBoxPlaceholders() {
        return getPlaceholderAPIHook().map(PlaceholderAPIHook::getBentoBoxPlaceholders).orElse(Set.of());
    }

    /**
     * Returns the set of placeholder identifiers registered by the given addon.
     * @param addon the addon, not null.
     * @return unmodifiable set of placeholder identifiers, or empty set if none.
     * @since 3.2.0
     */
    @NonNull
    public Set<String> getRegisteredPlaceholders(@NonNull Addon addon) {
        return getPlaceholderAPIHook().map(h -> h.getAddonPlaceholders(addon)).orElse(Set.of());
    }

    /**
     * Returns all addons that have at least one registered placeholder.
     * @return unmodifiable set of addons, or empty set if PlaceholderAPI is not hooked.
     * @since 3.2.0
     */
    @NonNull
    public Set<Addon> getAddonsWithPlaceholders() {
        return getPlaceholderAPIHook().map(PlaceholderAPIHook::getAddonsWithPlaceholders).orElse(Set.of());
    }

    /**
     * Returns the description for a BentoBox-core placeholder.
     * @param placeholder the placeholder identifier.
     * @return Optional containing the plain-English description, or empty if none was registered.
     * @since 3.2.0
     */
    @NonNull
    public Optional<String> getPlaceholderDescription(@NonNull String placeholder) {
        return getPlaceholderAPIHook().flatMap(h -> h.getDescription(placeholder));
    }

    /**
     * Returns the description for an addon placeholder.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier.
     * @return Optional containing the plain-English description, or empty if none was registered.
     * @since 3.2.0
     */
    @NonNull
    public Optional<String> getPlaceholderDescription(@NonNull Addon addon, @NonNull String placeholder) {
        return getPlaceholderAPIHook().flatMap(h -> h.getDescription(addon, placeholder));
    }

    // -------------------------------------------------------------------------
    // Enable / disable
    // -------------------------------------------------------------------------

    /**
     * Sets whether a BentoBox-core placeholder is enabled.
     * Disabled placeholders return an empty string instead of their resolved value.
     * This state is not persisted and resets on server restart.
     * @param placeholder the placeholder identifier, not null.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @since 3.2.0
     */
    public void setPlaceholderEnabled(@NonNull String placeholder, boolean enabled) {
        getPlaceholderAPIHook().ifPresent(h -> h.setEnabled(placeholder, enabled));
    }

    /**
     * Sets whether an addon placeholder is enabled.
     * Disabled placeholders return an empty string instead of their resolved value.
     * This state is not persisted and resets on server restart.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier, not null.
     * @param enabled {@code true} to enable, {@code false} to disable.
     * @since 3.2.0
     */
    public void setPlaceholderEnabled(@NonNull Addon addon, @NonNull String placeholder, boolean enabled) {
        getPlaceholderAPIHook().ifPresent(h -> h.setEnabled(addon, placeholder, enabled));
    }

    /**
     * Returns whether a BentoBox-core placeholder is currently enabled.
     * @param placeholder the placeholder identifier.
     * @return {@code true} if enabled (also true when PlaceholderAPI is not hooked).
     * @since 3.2.0
     */
    public boolean isPlaceholderEnabled(@NonNull String placeholder) {
        return getPlaceholderAPIHook().map(h -> h.isEnabled(placeholder)).orElse(true);
    }

    /**
     * Returns whether an addon placeholder is currently enabled.
     * @param addon the addon, not null.
     * @param placeholder the placeholder identifier.
     * @return {@code true} if enabled (also true when PlaceholderAPI is not hooked).
     * @since 3.2.0
     */
    public boolean isPlaceholderEnabled(@NonNull Addon addon, @NonNull String placeholder) {
        return getPlaceholderAPIHook().map(h -> h.isEnabled(addon, placeholder)).orElse(true);
    }

    // -------------------------------------------------------------------------
    // Replacement
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Flag placeholders
    // -------------------------------------------------------------------------

    /**
     * Registers flag placeholders for all currently registered flags.
     * <p>
     * For each flag, a placeholder named {@code flag_<FLAG_ID_LOWERCASE>} is registered.
     * The value depends on the flag type:
     * <ul>
     *     <li>{@link Flag.Type#PROTECTION} — returns the translated rank name of the minimum rank allowed</li>
     *     <li>{@link Flag.Type#SETTING} — returns {@code true} or {@code false}</li>
     *     <li>{@link Flag.Type#WORLD_SETTING} — returns {@code true} or {@code false}</li>
     * </ul>
     *
     * @param addon the game mode addon to register flag placeholders for
     * @since 3.13.0
     */
    private void registerFlagPlaceholders(@NonNull GameModeAddon addon) {
        if (plugin.getFlagsManager() == null) {
            return;
        }
        plugin.getFlagsManager().getFlags().forEach(flag -> registerFlagPlaceholder(addon, flag));
    }

    /**
     * Registers a single flag placeholder for a game mode addon.
     * <p>
     * The placeholder name is {@code flag_<FLAG_ID_LOWERCASE>}. If a placeholder
     * with that name is already registered for the addon, this method does nothing.
     *
     * @param addon the game mode addon
     * @param flag the flag to register a placeholder for
     * @since 3.13.0
     */
    public void registerFlagPlaceholder(@NonNull GameModeAddon addon, @NonNull Flag flag) {
        String placeholderName = "flag_" + flag.getID().toLowerCase(Locale.ENGLISH);
        if (isPlaceholder(addon, placeholderName)) {
            return;
        }
        String description = getFlagPlaceholderDescription(flag);
        registerPlaceholder(addon, placeholderName, description, user -> resolveFlagValue(addon, user, flag));
    }

    private String getFlagPlaceholderDescription(@NonNull Flag flag) {
        return switch (flag.getType()) {
            case PROTECTION ->
                    "Minimum rank required for " + flag.getID() + " on the player's island";
            case SETTING ->
                    "Whether " + flag.getID() + " is enabled on the player's island (true/false)";
            case WORLD_SETTING ->
                    "Whether " + flag.getID() + " is enabled in the world (true/false)";
        };
    }

    private String resolveFlagValue(@NonNull GameModeAddon addon, @Nullable User user, @NonNull Flag flag) {
        if (flag.getType() == Flag.Type.WORLD_SETTING) {
            return String.valueOf(flag.isSetForWorld(addon.getOverWorld()));
        }
        // SETTING and PROTECTION flags need the island
        if (user == null) {
            return "";
        }
        Island island = plugin.getIslands().getIsland(addon.getOverWorld(), user);
        if (island == null) {
            return "";
        }
        if (flag.getType() == Flag.Type.SETTING) {
            return String.valueOf(island.isAllowed(flag));
        }
        // PROTECTION flag - return the translated rank name
        int rankValue = island.getFlag(flag);
        String rankRef = RanksManager.getInstance().getRank(rankValue);
        if (rankRef.isEmpty()) {
            return "";
        }
        return user.getTranslationOrNothing(rankRef);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Default placeholder
     *
     */
    static class DefaultPlaceholder implements PlaceholderReplacer {
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
