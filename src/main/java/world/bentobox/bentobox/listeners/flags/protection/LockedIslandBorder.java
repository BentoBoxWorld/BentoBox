package world.bentobox.bentobox.listeners.flags.protection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Shows a player the edge of a locked island they have just been turned away from.
 *
 * <p>The border is a per-player vanilla world border drawn around the island's
 * protected area. It is purely visual, only the player who was turned away sees
 * it, and it is taken down again a few seconds after they stop trying to get in.
 * Whatever border the player had before (for example one drawn by the Border
 * addon) is put back afterwards.
 *
 * @author tastybento
 * @since 3.24.0
 */
class LockedIslandBorder {

    /** How long the border stays up after the last time the player was turned away */
    static final long SHOW_MILLIS = 5000L;
    private static final long MILLIS_PER_TICK = 50L;
    /** Long enough that a "moving" border never reaches its target while it is shown */
    private static final long MAX_TICKS = 107374182L;

    private final Map<UUID, Shown> shown = new HashMap<>();

    /** A border currently on a player's screen */
    private static final class Shown {
        private final String islandId;
        private final WorldBorder border;
        private final @Nullable WorldBorder previous;
        private long expires;

        Shown(String islandId, WorldBorder border, @Nullable WorldBorder previous, long expires) {
            this.islandId = islandId;
            this.border = border;
            this.previous = previous;
            this.expires = expires;
        }
    }

    /**
     * Shows the island's border to the player, or keeps it up for longer if it is already showing.
     * Does nothing if the feature is turned off in the config.
     * @param player player who was turned away
     * @param island the locked island
     * @param world the world the player is in, which may be the island's nether or end
     */
    void show(@NonNull Player player, @NonNull Island island, @NonNull World world) {
        Settings settings = BentoBox.getInstance().getSettings();
        if (!settings.isLockedBorder()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long expires = System.currentTimeMillis() + SHOW_MILLIS;
        Shown current = shown.get(uuid);
        if (current != null && current.islandId.equals(island.getUniqueId())
                && current.border.equals(player.getWorldBorder())) {
            // Already on screen. The pending removal task will see the new expiry time.
            current.expires = expires;
            return;
        }
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(island.getProtectionCenter().toVector().toLocation(world));
        double size = island.getProtectionRange() * 2D;
        border.setSize(size);
        border.setWarningDistance(0);
        // The client colors a border by whether it is shrinking, growing or still
        Settings.BorderColor color = settings.getLockedBorderColor();
        if (color == Settings.BorderColor.GREEN) {
            border.changeSize(size + 0.1, MAX_TICKS);
        } else if (color != Settings.BorderColor.BLUE) {
            border.changeSize(size - 0.1, MAX_TICKS);
        }
        // Keep the border the player had before any of ours went up
        WorldBorder previous = current != null ? current.previous : player.getWorldBorder();
        player.setWorldBorder(border);
        shown.put(uuid, new Shown(island.getUniqueId(), border, previous, expires));
        removeLater(uuid, SHOW_MILLIS);
    }

    private void removeLater(UUID uuid, long millis) {
        Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> remove(uuid), millis / MILLIS_PER_TICK + 1);
    }

    /**
     * Takes the border down if its time is up, otherwise checks again when it will be.
     */
    private void remove(UUID uuid) {
        Shown current = shown.get(uuid);
        if (current == null) {
            return;
        }
        long remaining = current.expires - System.currentTimeMillis();
        if (remaining > 0) {
            removeLater(uuid, remaining);
            return;
        }
        shown.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        // Leave it alone if something else has replaced our border in the meantime
        if (player != null && current.border.equals(player.getWorldBorder())) {
            player.setWorldBorder(current.previous);
        }
    }

    /**
     * Stops tracking a player, e.g. because they have logged out.
     * @param uuid player's UUID
     */
    void forget(UUID uuid) {
        shown.remove(uuid);
    }
}
