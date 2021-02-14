package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles harvesting
 * @author tastybento
 * @since 1.16.0
 */
public class HarvestListener extends FlagListener {

    /**
     * Handle visitor harvesting, e.g. honey
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public boolean onHarvest(PlayerHarvestBlockEvent e) {
        return checkIsland(e, e.getPlayer(), e.getHarvestedBlock().getLocation(), Flags.HARVEST);
    }
}
