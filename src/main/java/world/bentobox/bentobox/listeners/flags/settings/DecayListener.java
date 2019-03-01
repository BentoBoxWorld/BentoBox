package world.bentobox.bentobox.listeners.flags.settings;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.LeavesDecayEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

import java.util.Optional;

/**
 * Handles {@link world.bentobox.bentobox.lists.Flags#LEAF_DECAY}.
 * @author Poslovitch
 * @since 1.3.1
 */
public class DecayListener extends FlagListener {

    /**
     * Prevents leaves from decaying.
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (!getIWM().inWorld(e.getBlock().getLocation())) {
            return;
        }

        Optional<Island> island = getIslands().getIslandAt(e.getBlock().getLocation());
        // Cancel the event if needed - this means if this is not allowed on the island or in the world.
        e.setCancelled(island.map(i -> !i.isAllowed(Flags.LEAF_DECAY)).orElse(!Flags.LEAF_DECAY.isSetForWorld(e.getBlock().getWorld())));
    }
}
