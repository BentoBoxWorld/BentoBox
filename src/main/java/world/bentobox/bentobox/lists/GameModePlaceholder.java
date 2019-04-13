package world.bentobox.bentobox.lists;

import world.bentobox.bentobox.api.placeholders.GameModePlaceholderReplacer;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

public enum GameModePlaceholder {

    /* World-related */
    WORLD_FRIENDLY_NAME("world_friendly_name", (addon, user, island) -> addon.getWorldSettings().getFriendlyName()),
    /**
     * Displays the amount of islands in the world.
     * @since 1.5.0
     */
    WORLD_ISLANDS("world_islands", (addon, user, island) -> String.valueOf(addon.getIslands().getIslandCount(addon.getOverWorld()))),

    /* Island-related */
    ISLAND_DISTANCE("island_distance", (addon, user, island) -> island == null ? "" : DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())))),
    ISLAND_PROTECTION_RANGE("island_protection_range", (addon, user, island) -> String.valueOf(addon.getWorldSettings().getIslandDistance())),
    ISLAND_OWNER("island_owner", (addon, user, island) -> island == null ? "" : addon.getPlayers().getName(island.getOwner())),
    ISLAND_CREATION_DATE("island_creation_date", (addon, user, island) -> island == null ? "" : String.valueOf(island.getProtectionRange())),
    ISLAND_SPAWNPOINT("island_spawnpoint", (addon, user, island) -> island == null ? "" : Util.xyz(island.getCenter().toVector())),
    ISLAND_NAME("island_name", (addon, user, island) -> island == null ? "" : (island.getName() == null ? "" : island.getName())),
    /**
     * Displays the coordinates of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER("island_center", (addon, user, island) -> island == null ? "" : Util.xyz(island.getCenter().toVector())),
    /**
     * Displays the X coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_X("island_center_x", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockX())),
    /**
     * Displays the Y coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Y("island_center_y", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockY())),
    /**
     * Displays the Z coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Z("island_center_z", (addon, user, island) -> island == null ? "" : String.valueOf(island.getCenter().getBlockZ())),
    /**
     * Displays the maximum number of members the island can have
     * @since 1.5.0
     */
    ISLAND_MEMBERS_MAX("island_members_max", (addon, user, island) -> island == null ? "" : String.valueOf(user.getPermissionValue(addon.getPermissionPrefix() + "team.maxsize", addon.getPlugin().getIWM().getMaxTeamSize(addon.getOverWorld())))),
    /**
     * Displays the amount of players that are at least MEMBER on this island.
     * @since 1.5.0
     */
    ISLAND_MEMBERS_COUNT("island_members_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet().size())),
    /**
     * Displays the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_TRUSTEES_COUNT("island_trustees_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.TRUSTED_RANK, false).size())),
    /**
     * Displays the amount of players that are TRUSTED on this island.
     * @since 1.5.0
     */
    ISLAND_COOPS_COUNT("island_coops_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getMemberSet(RanksManager.COOP_RANK, false).size())),
    /**
     * Displays the amount of players that are currently visiting the island.
     * @since 1.5.0
     */
    ISLAND_VISITORS_COUNT("island_visitors_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getVisitors().size())),
    /**
     * Displays the amount of players banned from the island.
     * @since 1.5.0
     */
    ISLAND_BANS_COUNT("island_bans_count", (addon, user, island) -> island == null ? "" : String.valueOf(island.getBanned().size())),

    /* Player-related */
    /**
     * Displays whether this player has an island or not.
     * @since 1.5.0
     */
    HAS_ISLAND("has_island", (addon, user, island) -> String.valueOf(island != null)),
    /**
     * Displays the rank this player has on his island.
     * @since 1.5.0
     */
    RANK("rank", (addon, user, island) -> (island == null || user == null) ? "" : addon.getPlugin().getLocalesManager().get(user, addon.getPlugin().getRanksManager().getRank(island.getRank(user)))),
    /**
     * Displays how many times this player reset his island.
     * @since 1.5.0
     */
    RESETS("resets", (addon, user, island) -> String.valueOf(addon.getPlayers().getResets(addon.getOverWorld(), user.getUniqueId()))),
    /**
     * Displays how many times this player can reset his island.
     * {@code -1} is unlimited.
     * @since 1.5.0
     */
    RESETS_LEFT("resets_left", (addon, user, island) -> String.valueOf(addon.getPlayers().getResetsLeft(addon.getOverWorld(), user.getUniqueId())));

    private String placeholder;
    /**
     * @since 1.5.0
     */
    private GameModePlaceholderReplacer replacer;

    GameModePlaceholder(String placeholder, GameModePlaceholderReplacer replacer) {
        this.placeholder = placeholder;
        this.replacer = replacer;
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
