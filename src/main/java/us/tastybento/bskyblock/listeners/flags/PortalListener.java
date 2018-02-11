/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles portal protection
 * @author tastybento
 *
 */
public class PortalListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent e) {
        checkIsland(e, e.getFrom(), Flags.PORTAL);
    }
}
