package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles teleporting due to enderpearl or chorus fruit.
 * @author tastybento
 *
 */
public class TeleportationListener extends FlagListener {

    /**
     * Ender pearl and chorus fruit teleport checks
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent e) {
        if (e.getCause().equals(TeleportCause.ENDER_PEARL)) {
            checkIsland(e, e.getPlayer(), e.getTo(), Flags.ENDER_PEARL);
        } else if (e.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
            checkIsland(e, e.getPlayer(), e.getTo(), Flags.CHORUS_FRUIT);
        }
    }
}