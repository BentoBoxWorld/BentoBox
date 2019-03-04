package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link Flags#OFFLINE_GROWTH} flag.
 * @author Poslovitch
 * @since 1.4.0
 */
public class OfflineGrowthListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getBlock().getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        if (!(e.getBlock() instanceof Ageable)) {
            // Do nothing if the block is not "ageable" (and therefore not a crop).
            return;
        }

        e.setCancelled(true);
    }
}
