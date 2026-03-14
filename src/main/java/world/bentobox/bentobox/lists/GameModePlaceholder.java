package world.bentobox.bentobox.lists;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.api.placeholders.GameModePlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Common Game Mode Placeholders.
 * All of these are prefixed with the game mode's name, e.g., {@code bskykblock_}.
 * <p>
 * Each constant carries a plain-English {@link #getDescription() description} that is used
 * in the Placeholder GUI and the {@code /bbox dump-placeholders} output.
 * Descriptions are intentionally in English and are <em>not</em> locale keys.
 * </p>
 */
public enum GameModePlaceholder {

    /**
     * Returns how many times this player died.
     * @since 1.12.0
     */
    DEATHS("deaths",
            "Number of times the player has died",
            (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getDeaths(addon.getOverWorld(), user.getUniqueId()))),
    /* Player-related */
    /**
     * Returns whether this player has an island or not.
     * @since 1.5.0
     */
    HAS_ISLAND("has_island",
            "Whether the player has an island (true/false)",
            (addon, user, island) -> String.valueOf(user != null && island != null)),

    /* Island-related */

    /**
     * Returns the amount of players banned from the island.
     * @since 1.5.0
     */
    ISLAND_BANS_COUNT("island_bans_count",
            "Number of players banned from the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getBanned().size())),
    /**
     * Returns the coordinates of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER("island_center",
            "Coordinates of the player's island center (x,y,z)",
            (addon, user, island) -> island == null ? "" : Util.xyz(island.getCenter().toVector())),
    /**
     * Returns the X coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_X("island_center_x",
            "X coordinate of the player's island center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockX())),
    /**
     * Returns the Y coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Y("island_center_y",
            "Y coordinate of the player's island center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockY())),
    /**
     * Returns the Z coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Z("island_center_z",
            "Z coordinate of the player's island center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockZ())),
    /**
     * Returns a comma separated list of player names that are at least COOP on this island.
     * @since 2.4.2
     */
    ISLAND_COOP_LIST("island_coop_list",
            "Comma-separated list of coop players on the player's island",
            (addon, user, island) -> island == null ? ""
                    : island.getMemberSet(RanksManager.COOP_RANK, false).stream().map(addon.getPlayers()::getName)
                            .collect(Collectors.joining(","))),
    /**
     * Returns the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_COOPS_COUNT("island_coops_count",
            "Number of coop players on the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.COOP_RANK, false).size())),
    /**
     * Island creation date.
     * @since 3.2.0
     */
    ISLAND_CREATION_DATE("island_creation_date",
            "Creation date of the player's island",
            (addon, user, island) -> island == null ? ""
                    : DateFormat.getDateInstance(DateFormat.SHORT)
                            .format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())))),

    /**
     * Island creation time.
     * @since 3.2.0
     */
    ISLAND_CREATION_TIME("island_creation_time",
            "Creation time of the player's island",
            (addon, user, island) -> island == null ? ""
                    : DateFormat.getTimeInstance(DateFormat.SHORT)
                            .format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())))),
    /**
     * This is a fixed value from settings.
     */
    ISLAND_DISTANCE("island_distance",
            "Island spacing distance (half the distance between island centers)",
            (addon, user, island) -> String.valueOf(addon.getWorldSettings().getIslandDistance())),
    /**
     * Returns the island distance as a diameter (it is therefore equivalent to twice the island distance).
     * @since 1.5.0
     */
    ISLAND_DISTANCE_DIAMETER("island_distance_diameter",
            "Island spacing diameter (twice the island distance)",
            (addon, user, island) -> String.valueOf(2 * addon.getWorldSettings().getIslandDistance())),
    /**
     * Returns the number of players that are or have ever been a MEMBER on this island.
     * @since 3.0.0
     */
    ISLAND_HISTORICAL_MEMBERS_COUNT("island_historical_members_count",
            "Number of players who have ever been a member of the player's island",
            (addon, user, island) -> island == null ? "" : getHistoricalMembers(island)),
    /**
     * Returns the coordinates of the island's location, which may be different to the center.
     * @since 1.16.0
     */
    ISLAND_LOCATION("island_location",
            "Coordinates of the player's island protection center (x,y,z)",
            (addon, user, island) -> island == null ? "" : Util.xyz(island.getProtectionCenter().toVector())),
    /**
     * Returns the X coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_X("island_location_x",
            "X coordinate of the player's island protection center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockX())),
    /**
     * Returns the Y coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_Y("island_location_y",
            "Y coordinate of the player's island protection center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockY())),
    /**
     * Returns the Z coordinate of the island's location.
     * @since 1.16.0
     */
    ISLAND_LOCATION_Z("island_location_z",
            "Z coordinate of the player's island protection center",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionCenter().getBlockZ())),
    /**
     * Returns the max homes for this island.
     * @since 2.6.0
     */
    ISLAND_MAX_HOMES("island_max_homes",
            "Maximum number of homes allowed on the player's island",
            (addon, user, island) -> island == null ? ""
                    : String.valueOf(
                            island.getMaxHomes() == null ? addon.getPlugin().getIWM().getMaxHomes(island.getWorld())
                                    : island.getMaxHomes())),
    /**
     * Returns the amount of players that are at least MEMBER on this island.
     * @since 1.5.0
     */
    ISLAND_MEMBERS_COUNT("island_members_count",
            "Number of members (rank >= member) on the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet().size())),
    /**
     * Returns a comma separated list of player names that are at least MEMBER on this island.
     * @since 1.13.0
     */
    ISLAND_MEMBERS_LIST("island_members_list",
            "Comma-separated list of member-ranked players on the player's island",
            (addon, user, island) -> island == null ? "" : island.getMemberSet(RanksManager.MEMBER_RANK).stream()
            .map(addon.getPlayers()::getName).collect(Collectors.joining(","))),

    /**
     * Returns the maximum number of members the island can have.
     * @since 1.5.0
     */
    ISLAND_MEMBERS_MAX("island_members_max",
            "Maximum number of members allowed on the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(addon.getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK))),

    /**
     * Returns the island name.
     */
    ISLAND_NAME("island_name",
            "Name of the player's island",
            (addon, user, island) -> {
        if (island == null || user == null) {
            return "";
        }
        if (island.getName() == null) {
            return user.getTranslation(island.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, addon.getPlayers().getName(island.getOwner()));
        }
        return island.getName();
    }),
    /**
     * Returns the island owner's name.
     */
    ISLAND_OWNER("island_owner",
            "Name of the owner of the player's island",
            (addon, user, island) -> island == null ? "" : addon.getPlayers().getName(island.getOwner())),
    // ----------------------------------
    /**
     * Returns the island's protection range.
     * @since 1.4.0
     */
    ISLAND_PROTECTION_RANGE("island_protection_range",
            "Protection range (radius) of the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionRange())),
    /**
     * Returns the island's protection range as a diameter (it is therefore equivalent to twice the island protection range).
     * @since 1.5.0
     */
    ISLAND_PROTECTION_RANGE_DIAMETER("island_protection_range_diameter",
            "Protection diameter (2x protection range) of the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(2 * island.getProtectionRange())),
    /**
     * Get the rank the user has on their island.
     */
    ISLAND_RANK("island_rank",
            "Rank of the player on their own island",
            (addon, user, island) -> island == null ? ""
                    : user.getTranslationOrNothing(RanksManager.getInstance().getRank(island.getRank(user)))),
    /**
     * Returns a comma separated list of player names that are at least TRUSTED on this island.
     * @since 2.4.2
     */
    ISLAND_TRUSTED_LIST("island_trusted_list",
            "Comma-separated list of trusted players on the player's island",
            (addon, user, island) -> island == null ? ""
                    : island.getMemberSet(RanksManager.TRUSTED_RANK, false).stream().map(addon.getPlayers()::getName)
                            .collect(Collectors.joining(","))),
    /**
     * Returns the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_TRUSTEES_COUNT("island_trustees_count",
            "Number of trusted players on the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.TRUSTED_RANK, false).size())),
    /**
     * Return island unique ID.
     * @since 1.15.4
     */
    ISLAND_UUID("island_uuid",
            "Unique ID of the player's island",
            (addon, user, island) -> island == null ? "" : island.getUniqueId()),
    /**
     * Returns the amount of players that are currently visiting the island.
     * @since 1.5.0
     */
    ISLAND_VISITORS_COUNT("island_visitors_count",
            "Number of players currently visiting the player's island",
            (addon, user, island) -> island == null ? "" : String.valueOf(island.getVisitors().size())),

    /**
     * Returns whether this player is on his island and has a rank greater than VISITOR_RANK.
     * @since 1.13.0
     */
    ON_ISLAND("on_island",
            "Whether the player is currently on their own island (true/false)",
            (addon, user, island) -> String.valueOf(addon.getIslands().userIsOnIsland(addon.getOverWorld(), user)
                            || addon.getIslands().userIsOnIsland(addon.getNetherWorld(), user)
                            || addon.getIslands().userIsOnIsland(addon.getEndWorld(), user))),
    /**
     * Returns whether this player is an owner of their island.
     * @since 1.14.0
     */
    OWNS_ISLAND("owns_island",
            "Whether the player is the owner of their island (true/false)",
            (addon, user, island) -> String.valueOf(island != null && user != null && user.getUniqueId().equals(island.getOwner()))),
    /**
     * Returns the rank this player has on his island.
     * @since 1.5.0
     */
    RANK("rank",
            "Translated rank name of the player on their island",
            (addon, user, island) -> (island == null || user == null) ? ""
                    : user.getTranslation(RanksManager.getInstance().getRank(island.getRank(user)))),
    /**
     * Returns how many times this player reset his island.
     * @since 1.5.0
     */
    RESETS("resets",
            "Number of times the player has reset their island",
            (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getResets(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Returns how many times this player can reset his island.
     * {@code -1} is unlimited.
     * @since 1.5.0
     */
    RESETS_LEFT("resets_left",
            "Number of island resets the player has remaining (-1 = unlimited)",
            (addon, user, island) -> user == null ? "" : String.valueOf(addon.getPlayers().getResetsLeft(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Returns the amount of players banned from the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_BANS_COUNT("visited_island_bans_count",
            "Number of players banned from the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getBanned().size())).orElse("")),
    /**
     * Returns the coordinates of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER("visited_island_center",
            "Coordinates of the center of the island the player is standing on (x,y,z)",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> Util.xyz(value.getCenter().toVector())).orElse("")),
    /**
     * Returns the X coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_X("visited_island_center_x",
            "X coordinate of the center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockX())).orElse("")),
    /**
     * Returns the Y coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_Y("visited_island_center_y",
            "Y coordinate of the center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockY())).orElse("")),
    /**
     * Returns the Z coordinate of the center of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CENTER_Z("visited_island_center_z",
            "Z coordinate of the center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getCenter().getBlockZ())).orElse("")),
    /**
     * Returns a comma separated list of player names that are COOP on the island the player is standing on.
     * @since 2.4.2
     */
    VISITED_ISLAND_COOP_LIST("visited_island_coop_list",
            "Comma-separated list of coop players on the island the player is standing on",
            (addon, user, island) -> getVisitedIsland(addon, user).map(value -> value.getMemberSet(RanksManager.COOP_RANK, false)
                    .stream().map(addon.getPlayers()::getName).collect(Collectors.joining(","))).orElse("")),
    /**
     * Returns the amount of players that are TRUSTED on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_COOPS_COUNT("visited_island_coops_count",
            "Number of coop players on the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet(RanksManager.COOP_RANK, false).size())).orElse("")),
    /**
     * Returns the formatted creation date of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_CREATION_DATE("visited_island_creation_date",
            "Creation date of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(value.getCreatedDate())))).orElse("")),
    /**
     * Returns the coordinates of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION("visited_island_location",
            "Coordinates of the protection center of the island the player is standing on (x,y,z)",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> Util.xyz(value.getProtectionCenter().toVector())).orElse("")),
    /**
     * Returns the X coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_X("visited_island_location_x",
            "X coordinate of the protection center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockX())).orElse("")),
    /**
     * Returns the Y coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_Y("visited_island_location_y",
            "Y coordinate of the protection center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockY())).orElse("")),
    /**
     * Returns the Z coordinate of the location of the island the player is standing on.
     * @since 1.16.0
     */
    VISITED_ISLAND_LOCATION_Z("visited_island_location_z",
            "Z coordinate of the protection center of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionCenter().getBlockZ())).orElse("")),
    /**
     * Returns the max homes of the island the player is standing on.
     * @since 2.6.0
     */
    VISITED_ISLAND_MAX_HOMES("visited_island_max_homes",
            "Maximum number of homes on the island the player is standing on",
            (addon, user, island) -> getVisitedIsland(addon, user).map(value -> String.valueOf(
                            island.getMaxHomes() == null ? addon.getPlugin().getIWM().getMaxHomes(island.getWorld())
                                    : island.getMaxHomes()))
                            .orElse("")),
    /**
     * Returns the amount of players that are at least MEMBER on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_MEMBERS_COUNT("visited_island_members_count",
            "Number of members on the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet().size())).orElse("")),
    /**
     * Returns a comma separated list of player names that are at least MEMBER on the island the player is standing on.
     * @since 1.13.0
     */
    VISITED_ISLAND_MEMBERS_LIST("visited_island_members_list",
            "Comma-separated list of member-ranked players on the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> value.getMemberSet(RanksManager.MEMBER_RANK).stream()
            .map(addon.getPlayers()::getName).collect(Collectors.joining(","))).orElse("")),
    /**
     * Returns the maximum number of members the island the player is standing on can have.
     * @since 1.5.2
     */
    VISITED_ISLAND_MEMBERS_MAX("visited_island_members_max",
            "Maximum number of members allowed on the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(addon.getIslands().getMaxMembers(value, RanksManager.MEMBER_RANK)))
    .orElse("")),
    /**
     * Returns the name of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_NAME("visited_island_name",
            "Name of the island the player is standing on",
            (addon, user, island) -> getVisitedIsland(addon, user).map(is -> {
        if (is.getName() != null) {
            return is.getName();
        } else {
            return user.getTranslation(is.getWorld(), "protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, addon.getPlayers().getName(is.getOwner()));
        }
    }).orElse("")),
    /**
     * Returns the name of the owner of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_OWNER("visited_island_owner",
            "Name of the owner of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> addon.getPlayers().getName(value.getOwner())).orElse("")),

    /* Visited island-related (= island the user is standing on) */
    /**
     * Returns the protection range of the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_PROTECTION_RANGE("visited_island_protection_range",
            "Protection range (radius) of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getProtectionRange())).orElse("")),

    /**
     * Returns the protection range of the island the player is standing on as a diameter.
     * @since 1.5.2
     */
    VISITED_ISLAND_PROTECTION_RANGE_DIAMETER("visited_island_protection_range_diameter",
            "Protection diameter (2x protection range) of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(2*value.getProtectionRange())).orElse("")),
    /**
     * Returns the rank this player has on this island.
     * @since 2.4.0
     */
    VISITED_ISLAND_RANK("visited_island_rank",
            "Rank of the player on the island they are standing on",
            (addon, user, island) -> getVisitedIsland(addon, user)
                    .map(is -> user.getTranslation(RanksManager.getInstance().getRank(is.getRank(user)))).orElse("")),
    /**
     * Returns a comma separated list of player names that are TRUSTED on the island the player is standing on.
     * @since 2.4.2
     */
    VISITED_ISLAND_TRUSTED_LIST("visited_island_trusted_list",
            "Comma-separated list of trusted players on the island the player is standing on",
            (addon, user, island) -> getVisitedIsland(addon, user).map(value -> value.getMemberSet(RanksManager.TRUSTED_RANK, false)
                    .stream().map(addon.getPlayers()::getName).collect(Collectors.joining(","))).orElse("")),

    /**
     * Returns the amount of players that are TRUSTED on the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_TRUSTEES_COUNT("visited_island_trustees_count",
            "Number of trusted players on the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getMemberSet(RanksManager.TRUSTED_RANK, false).size())).orElse("")),
    /**
     * Get the visited island unique ID.
     * @since 1.15.4
     */
    VISITED_ISLAND_UUID("visited_island_uuid",
            "Unique ID of the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(Island::getUniqueId).orElse("")),
    /**
     * Returns the amount of players that are currently visiting the island the player is standing on.
     * @since 1.5.2
     */
    VISITED_ISLAND_VISITORS_COUNT("visited_island_visitors_count",
            "Number of players currently visiting the island the player is standing on",
            (addon, user, island) ->
    getVisitedIsland(addon, user).map(value -> String.valueOf(value.getVisitors().size())).orElse("")),
    /* World-related */
    /**
     * World friendly name.
     */
    WORLD_FRIENDLY_NAME("world_friendly_name",
            "Friendly display name of the game mode world",
            (addon, user, island) -> addon.getWorldSettings().getFriendlyName()),
    /**
     * Returns the amount of islands in the world.
     * @since 1.5.0
     */
    WORLD_ISLANDS("world_islands",
            "Total number of islands in the game mode world",
            (addon, user, island) -> String.valueOf(addon.getIslands().getIslandCount(addon.getOverWorld())));

    /**
     * Provides a count of how many players have ever joined the island as a member including the owner.
     * @param island island
     * @return String count of the number of members
     */
    private static String getHistoricalMembers(@Nullable Island island) {
        if (island == null) {
            return "0";
        }
        Set<String> uniqueMembers = new HashSet<>();
        for (LogEntry le : island.getHistory()) {
            if (le.getType() == LogType.JOINED) {
                uniqueMembers.addAll(le.getData().keySet());
            }
        }
        return String.valueOf(uniqueMembers.size());
    }
    /**
     * Get the visited island.
     * @param addon - game mode addon
     * @param user - user visiting
     * @return optional island
     */
    private static Optional<Island> getVisitedIsland(@NonNull GameModeAddon addon, @Nullable User user) {
        if (user == null || !user.isPlayer()) {
            return Optional.empty();
        } else {
            user.getLocation();
        }
        return addon.getIslands().getIslandAt(user.getLocation());
    }

    private final String placeholder;

    /**
     * A plain-English description of what this placeholder returns.
     * This is intentionally not a locale key.
     */
    private final String description;

    /**
     * @since 1.5.0
     */
    private final GameModePlaceholderReplacer replacer;

    GameModePlaceholder(String placeholder, String description, GameModePlaceholderReplacer replacer) {
        this.placeholder = placeholder;
        this.description = description;
        this.replacer = replacer;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Returns a plain-English description of what this placeholder returns.
     * This is not a locale key.
     * @return the description, never null.
     * @since 3.2.0
     */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * @since 1.5.0
     */
    public GameModePlaceholderReplacer getReplacer() {
        return replacer;
    }
}
