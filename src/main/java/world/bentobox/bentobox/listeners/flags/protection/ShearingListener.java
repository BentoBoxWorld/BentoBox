package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles shearing
 * @author tastybento
 *
 */
public class ShearingListener extends FlagListener {

    public ShearingListener() {
        Bukkit.getPluginManager().registerEvents(new PaperShearingListener(), getPlugin());
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
        checkIsland(e, e.getPlayer(), e.getEntity().getLocation(), Flags.SHEARING);
    }

}
