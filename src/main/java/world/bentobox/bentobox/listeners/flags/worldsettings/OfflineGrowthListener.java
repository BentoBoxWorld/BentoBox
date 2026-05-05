package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles {@link Flags#OFFLINE_GROWTH} flag.
 * @author Poslovitch, tastybento
 * @since 1.4.0
 */
public class OfflineGrowthListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getBlock().getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }
        // Check if island exists and all members are offline.
        // BlockGrowEvent has no source/destination split — the block itself is the one growing.
        checkGrowth(e.getBlock().getLocation(), e);
    }

    /**
     * Handles all block spreading (vines, kelp, bamboo, etc.).
     * Uses the source block location so that plants growing outward from an island
     * are correctly associated with that island.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getBlock().getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }
        // Check if island exists and all members are offline - use source block location
        // so vines, kelp, bamboo and any other spreading plant are all caught
        checkGrowth(e.getSource().getLocation(), e);
    }

    /**
     * Handles tree and mushroom growth via {@link StructureGrowEvent}.
     * Trees (birch, spruce, acacia, mangrove, etc.) and mushrooms do not fire
     * {@link BlockGrowEvent} when growing from a sapling; they use this event instead.
     * @since 3.15.1
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        if (!getIWM().inWorld(e.getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }
        // Check if island exists and all members are offline
        checkGrowth(e.getLocation(), e);
    }

    private void checkGrowth(Location location, Cancellable event) {
        getIslands().getProtectedIslandAt(location).ifPresent(i -> {
            for (UUID uuid : i.getMemberSet(RanksManager.COOP_RANK)) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            event.setCancelled(true);
        });
    }
}
