/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import us.tastybento.bskyblock.lists.Flag;

/**
 * Handles teleporting due to enderpearl or chorus fruit.
 * @author tastybento
 *
 */
public class TeleportationListener extends AbstractFlagListener {

    /**
     * Ender pearl and chorus fruit teleport checks
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent e) {

        if (e.getCause() != null) {
            if (e.getCause().equals(TeleportCause.ENDER_PEARL)) {
                checkIsland(e, e.getTo(), Flag.ENDER_PEARL);
            } else if (e.getCause().equals(TeleportCause.CHORUS_FRUIT)) {
                checkIsland(e, e.getTo(), Flag.CHORUS_FRUIT);
            }
        }
    }
}