package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles portal protection
 * @author tastybento
 */
public class PortalListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (e.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            checkIsland(e, e.getPlayer(), e.getFrom(), Flags.NETHER_PORTAL);
        } else if (e.getCause().equals(TeleportCause.END_PORTAL)) {
            checkIsland(e, e.getPlayer(), e.getFrom(), Flags.END_PORTAL);
        }
    }
}
