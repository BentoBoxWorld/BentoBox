package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects candles
 * @author tastybento
 * @since 2.4.2
 */
public class CandleListener extends FlagListener {

    /**
     * Prevent dying signs.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCandleInteract(final PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }

        if (Tag.CANDLES.isTagged(e.getClickedBlock().getType())
                || Tag.CANDLE_CAKES.isTagged(e.getClickedBlock().getType())) {
            this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.CANDLES);
        }
    }
}
