package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * Sets the player's primary island based on where they teleported or moved to
 * @author tastybento
 *
 */
public class PrimaryIslandListener implements Listener {

    private final IslandsManager im;

    /**
     * @param plugin - plugin object
     */
    public PrimaryIslandListener(@NonNull BentoBox plugin) {
        this.im = plugin.getIslands();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        setIsland(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        // Island bounds are block-based, so only a block change can alter the result.
        // Compare raw coordinates to avoid allocating vectors on every move event.
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            setIsland(event.getPlayer(), to);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(final PlayerTeleportEvent event) {
        setIsland(event.getPlayer(), event.getTo());
    }

    private void setIsland(Player player, Location location) {
        im.getIslandAt(location)
        .filter(i -> player.getUniqueId().equals(i.getOwner()))
        .ifPresent(i -> im.setPrimaryIsland(player.getUniqueId(), i));
    }

}
