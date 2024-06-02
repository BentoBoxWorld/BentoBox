package world.bentobox.bentobox.lists;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.placeholders.GameModePlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Common Game Mode Placeholders
 */
public enum GameModePlaceholder {

    /* World-related */
    /**
     * World friendly name
     */
    WORLD_FRIENDLY_NAME("world_friendly_name", (addon, user, island) -> addon.getWorldSettings().getFriendlyName()),
    /**
     * Returns the amount of islands in the world.
     * @since 1.5.0
     */
    WORLD_ISLANDS("world_islands", (addon, user, island) -> String.valueOf(addon.getIslands().getIslandCount(addon.getOverWorld()))),

    /* Island-related */

    // TODO: change the two next placeholders as they're related to the worlds instead of the islands
    ISLAND_DISTANCE("island_distance", (addon, user, island) -> String.valueOf(addon.getWorldSettings().getIslandDistance())),
    /**
     * Returns the island distance as a diameter (it is therefore equivalent to twice the island distance).
     * @since 1.5.0
     */
    ISLAND_DISTANCE_DIAMETER("island_distance_diameter", (addon, user, island) -> String.valueOf(2 * addon.getWorldSettings().getIslandDistance())),
    // ----------------------------------
    /**
     * Returns the island's protection range.
     * @since 1.4.0
     */
    ISLAND_PROTECTION_RANGE("island_protection_range", (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionRange())),
    /**
     * Returns the island's protection range as a diameter (it is therefore equivalent to twice the island protection range).
     * @since 1.5.0
     */
    ISLAND_PROTECTION_RANGE_DIAMETER("island_protection_range_diameter", (addon, user, island) -> island == null ? "" : String.valueOf(2 * island.getProtectionRange())),
    ISLAND_OWNER("island_owner", (addon, user, island) -> island == null ? "" : addon.getPlayers().getName(island.getOwner())),
    ISLAND_CREATION_DATE("island_creation_date", (addon, user, island) -> island == null ? "" : DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())))),
    ISLAND_NAME("island_name", (addon, user, island) -> {
        if (island == null || user == null) {
            return "";
        }
        if (island.getName() == null) {
            return user.getTranslation(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, addon.getPlayers().getName(island.getOwner()));
        }
        return island.getName();
    }),
    /**
     * Return island unique ID
     * @since 1.15.4
     */
    ISLAND_UUID("island_uuid", (addon, user, island) -> island == null ? "" : island.getUniqueId()),
    /**
     * Returns the coordinates of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER("island_center", (addon, user, island) -> island == null ? "" : Util.xyz(island.getCenter().toVector())),
    /**
     * Returns the X coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_X("island_center_x", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockX())),
    /**
     * Returns the Y coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Y("island_center_y", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockY())),
    /**
     * Returns the Z coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Z("island_center_z", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockZ())),
    /**
     * Returns the coordinates of the island's location, which may be different to the center.
     * @since 1.16.0
     */
    ISLAND_LOCATION("island_location", (addon, user, island) -> island == null ? "" : Util.xyz(island.getProtectionCenter().toVector())),
    /**
     * Returns the X coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_X("island_location_x", (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockX())),
    /**
     * Returns the Y coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_Y("island_location_y", (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockY())),
    /**
     * Returns the Z coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_Z("island_location_z", (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockZ())),
    /**
     * Returns the maximum number of members the island can have
     * @since 1.5.0
     */
    ISLAND_MEMBERS_MAX("island_members_max", (addon, user, island) -> island == null ? "" : String.valueOf(addon.getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK))),
    /**
     * Returns the amount of players that are at least MEMBER on this island.
     * @since 1.5.0
     */
    ISLAND_MEMBERS_COUNT("island_members_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet().size())),
    /**
     * Returns a comma separated list of player names that are at least MEMBER on this island.
     * @since 1.13.0
     */
    ISLAND_MEMBERS_LIST("island_members_list", (addon, user, island) -> island == null ? "" : island.getMemberSet(RanksManager.MEMBER_RANK).stream()
            .map(addon.getPlayers()::getName).collect(Collectors.joining(","))),
    /**
     * Returns the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_TRUSTEES_COUNT("island_trustees_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.TRUSTED_RANK, false).size())),
    /**
     * Returns the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_COOPS_COUNT("island_coops_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.COOP_RANK, false).size())),
    /**
     * Returns the amount of players that are currently visiting the island.
     * @since 1.5.0
     */
    ISLAND_VISITORS_COUNT("island_visitors_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getVisitors().size())),
    /**
     * Returns the amount of players banned from the island.
     * @since 1.5.0
     */
    ISLAND_BANS_COUNT("island_bans_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getBanned().size())),

    /* Visited island-related (= island the user is standing on) */
    /**
     * Returns the protection range of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_PROTECTION_RANGE("visited_island_protection_range", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionRange())).orElse("")),
    /**
     * Returns the protection range of the island the player is standing on as a diameter.
     * @since 1.5.2
     */
    VISITED_ISLAND_PROTECTION_RANGE_DIAMETER("visited_island_protection_range_diameter", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(2*value.getProtectionRange())).orElse("")),
    /**
     * Returns the name of the owner of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_OWNER("visited_island_owner", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> addon.getPlayers().getName(value.getOwner())).orElse("")),
    /**
     * Returns the formatted creation date of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CREATION_DATE("visited_island_creation_date", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(value.getCreatedDate())))).orElse("")),
    /**
     * Returns the name of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_NAME("visited_island_name", (addon, user, island) -> getVisitedIsland(addon, user).map(is -> {
        if (is.getName() != null) {
            return is.getName();
        } else {
            return user.getTranslation(is.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, addon.getPlayers().getName(is.getOwner()));
        }
    }).orElse("")),
    /**
     * Returns the coordinates of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER("visited_island_center", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> Util.xyz(value.getCenter().toVector())).orElse("")),
    /**
     * Returns the X coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_X("visited_island_center_x", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockX())).orElse("")),
    /**
     * Returns the Y coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_Y("visited_island_center_y", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockY())).orElse("")),
    /**
     * Returns the Z coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_Z("visited_island_center_z", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockZ())).orElse("")),
    /**
     * Returns the coordinates of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION("visited_island_location", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> Util.xyz(value.getProtectionCenter().toVector())).orElse("")),
    /**
     * Returns the X coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_X("visited_island_location_x", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockX())).orElse("")),
    /**
     * Returns the Y coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_Y("visited_island_location_y", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockY())).orElse("")),
    /**
     * Returns the Z coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_Z("visited_island_location_z", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockZ())).orElse("")),
    /**
     * Returns the maximum number of members the island the player is standing on can have.
     * @since 1.5.2
     */
    VISITED_ISLAND_MEMBERS_MAX("visited_island_members_max", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(addon.getIslands().getMaxMembers(value, RanksManager.MEMBER_RANK)))
    .orElse("")),
    /**
     * Returns a comma separated list of player names that are at least MEMBER on the island the player is standing on.
     * @since 1.13.0
     */
    VISITED_ISLAND_MEMBERS_LIST("visited_island_members_list", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> value.getMemberSet(RanksManager.MEMBER_RANK).stream()
            .map(addon.getPlayers()::getName).collect(Collectors.joining(","))).orElse("")),
    /**
     * Returns the amount of players that are at least MEMBER on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_MEMBERS_COUNT("visited_island_members_count", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet().size())).orElse("")),
    /**
     * Returns the amount of players that are TRUSTED on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_TRUSTEES_COUNT("visited_island_trustees_count", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet(RanksManager.TRUSTED_RANK, false).size())).orElse("")),
    /**
     * Returns the amount of players that are TRUSTED on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_COOPS_COUNT("visited_island_coops_count", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet(RanksManager.COOP_RANK, false).size())).orElse("")),
    /**
     * Returns the amount of players that are currently visiting the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_VISITORS_COUNT("visited_island_visitors_count", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getVisitors().size())).orElse("")),
    /**
     * Returns the amount of players banned from the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_BANS_COUNT("visited_island_bans_count", (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getBanned().size())).orElse("")),

    /**
     * Get the visited island unique ID
     * @since 1.15.4
     */
    VISITED_ISLAND_UUID("visited_island_uuid", (addon, user, island) ->
    getVisitedIsland(addon, user).map(Island::getUniqueId).orElse("")),

    /* Player-related */
    /**
     * Returns whether this player has an island or not.
     * @since 1.5.0
     */
    HAS_ISLAND("has_island", (addon, user, island) -> String.valueOf(user != null && island != null)),
    /**
     * Returns the rank this player has on his island.
     * @since 1.5.0
     */
    RANK("rank",
            (addon, user, island) -> (island == null || user == null) ? ""
                    : user.getTranslation(RanksManager.getInstance().getRank(island.getRank(user)))),
    /**
     * Returns the rank this player has on this island.
     * @since 2.4.0
     */
    VISITED_ISLAND_RANK("visited_island_rank",
            (addon, user, island) -> getVisitedIsland(addon, user)
                    .map(is -> user.getTranslation(RanksManager.getInstance().getRank(is.getRank(user)))).orElse("")),

    /**
     * Returns how many times this player reset his island.
     * @since 1.5.0
     */
    RESETS("resets", (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getResets(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Returns how many times this player can reset his island.
     * {@code -1} is unlimited.
     * @since 1.5.0
     */
    RESETS_LEFT("resets_left", (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getResetsLeft(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Returns how many times this player died.
     * @since 1.12.0
     */
    DEATHS("deaths", (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getDeaths(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Returns whether this player is on his island and has a rank greater than VISITOR_RANK
     * @since 1.13.0
     */
    ON_ISLAND("on_island",
            (addon, user,
                    island) -> String.valueOf(addon.getIslands().userIsOnIsland(addon.getOverWorld(), user)
                            || addon.getIslands().userIsOnIsland(addon.getNetherWorld(), user)
                            || addon.getIslands().userIsOnIsland(addon.getEndWorld(), user))),
    /**
     * Returns whether this player is an owner of their island
     * @since 1.14.0
     */
    OWNS_ISLAND("owns_island", (addon, user, island) -> String.valueOf(island != null && user != null && user.getUniqueId().equals(island.getOwner())));

    private final String placeholder;
    /**
     * @since 1.5.0
     */
    private final GameModePlaceholderReplacer replacer;

    GameModePlaceholder(String placeholder, GameModePlaceholderReplacer replacer) {
        this.placeholder = placeholder;
        this.replacer = replacer;
    }

    /**
     * Get the visited island
     * @param addon - game mode addon
     * @param user - user visiting
     * @return optional island
     */
    private static Optional<Island> getVisitedIsland(@NonNull GameModeAddon addon, @Nullable User user) {
        if (user == null || !user.isPlayer() || user.getLocation() == null) {
            return Optional.empty();
        }
        return addon.getIslands().getIslandAt(user.getLocation());
    }

    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * @since 1.5.0
     */
    public GameModePlaceholderReplacer getReplacer() {
        return replacer;
    }
}
