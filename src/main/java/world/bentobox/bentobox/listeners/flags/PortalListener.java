package world.bentobox.bentobox.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles portal protection
 * @author tastybento
 *
 */
public class PortalListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (e.getPlayer().isOp()) {
            return;
        }
        if (e.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            checkIsland(e, e.getFrom(), Flags.PORTAL);
        } else if (e.getCause().equals(TeleportCause.END_PORTAL)) {
            // Silent check because it's spammy
            checkIsland(e, e.getFrom(), Flags.PORTAL, true);
        }
    }
}
