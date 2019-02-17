package world.bentobox.bentobox.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;

/**
 * Counts deaths in game worlds
 * @author tastybento
 *
 */
public class DeathListener implements Listener {

    private BentoBox plugin;

    public DeathListener(@NonNull BentoBox plugin) {
        super();
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (plugin.getIWM().inWorld(e.getEntity().getLocation()) && plugin.getIWM().getWorldSettings(e.getEntity().getLocation().getWorld()).isDeathsCounted()) {
            plugin.getPlayers().addDeath(e.getEntity().getWorld(), e.getEntity().getUniqueId());
        }
    }
}
