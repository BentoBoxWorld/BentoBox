package world.bentobox.bentobox.lists;

import world.bentobox.bentobox.api.placeholders.GameModePlaceholderReplacer;
import world.bentobox.bentobox.util.Util;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

public enum GameModePlaceholders {

    WORLD_FRIENDLY_NAME("world-friendlyname", (addon, user, island) -> addon.getWorldSettings().getFriendlyName()),
    ISLAND_DISTANCE("island-distance", (addon, user, island) -> DateFormat.getInstance().format(Date.from(Instant.ofEpochMilli(island.getCreatedDate())))),
    ISLAND_PROTECTION_RANGE("island-protection-range", (addon, user, island) -> String.valueOf(addon.getWorldSettings().getIslandDistance())),
    ISLAND_OWNER("island-owner", (addon, user, island) -> addon.getPlayers().getName(island.getOwner())),
    ISLAND_CREATION_DATE("island-creation-date", (addon, user, island) -> String.valueOf(island.getProtectionRange())),
    ISLAND_SPAWNPOINT("island-spawnpoint", (addon, user, island) -> Util.xyz(island.getCenter().toVector())),
    ISLAND_NAME("island-name", (addon, user, island) -> island.getName() == null ? "" : island.getName()),
    /**
     * Displays the X coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_X("island-center-x", (addon, user, island) -> String.valueOf(island.getCenter().getBlockX())),
    /**
     * Displays the Y coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Y("island-center-y", (addon, user, island) -> String.valueOf(island.getCenter().getBlockY())),
    /**
     * Displays the Z coordinate of the island's center.
     * @since 1.5.0
     */
    ISLAND_CENTER_Z("island-center-z", (addon, user, island) -> String.valueOf(island.getCenter().getBlockZ()));

    private String placeholder;
    /**
     * @since 1.5.0
     */
    private GameModePlaceholderReplacer replacer;

    GameModePlaceholders(String placeholder, GameModePlaceholderReplacer replacer) {
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
