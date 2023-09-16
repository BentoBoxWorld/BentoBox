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
        if (event.getTo() != null && !event.getFrom().toVector().equals(event.getTo().toVector())) {
            setIsland(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(final PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            setIsland(event.getPlayer(), event.getTo());
        }
    }

    private void setIsland(Player player, Location location) {
        im.getIslandAt(location)
        .filter(i -> i.getOwner() != null && i.getOwner().equals(player.getUniqueId()))
        .ifPresent(i -> im.setPrimaryIsland(player.getUniqueId(), i));
    }

}
