/**
 * 
 */
package world.bentobox.bentobox.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import world.bentobox.bentobox.BentoBox;

/**
 * Counts deaths in game worlds
 * @author tastybento
 *
 */
public class DeathListener implements Listener {

    private BentoBox plugin;

    public DeathListener(BentoBox plugin) {
        super();
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        if (plugin.getIWM().inWorld(e.getEntity().getLocation())) {
            plugin.getPlayers().addDeath(e.getEntity().getWorld(), e.getEntity().getUniqueId());
        }
    }
}
