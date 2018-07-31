package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles interaction with beds
 * Note - bed protection from breaking or placing is done elsewhere.
 * @author tastybento
 *
 */
public class BucketListener extends AbstractFlagListener {

    /**
     * Prevents emptying of buckets
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (e.getBlockClicked() != null) {
            // This is where the water or lava actually will be dumped
            Block dumpBlock = e.getBlockClicked().getRelative(e.getBlockFace());
            checkIsland(e, dumpBlock.getLocation(), Flags.BUCKET);
        }
    }

    /**
     * Prevents collecting of lava, water, milk. If bucket use is denied in general, it is blocked.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
        // Check filling of various liquids
        if (e.getItemStack().getType().equals(Material.LAVA_BUCKET) && (!checkIsland(e, e.getBlockClicked().getLocation(), Flags.COLLECT_LAVA))) {
            return;
        }
        if (e.getItemStack().getType().equals(Material.WATER_BUCKET) && (!checkIsland(e, e.getBlockClicked().getLocation(), Flags.COLLECT_WATER))) {
            return;
        }
        if (e.getItemStack().getType().equals(Material.MILK_BUCKET) && (!checkIsland(e, e.getBlockClicked().getLocation(), Flags.MILKING))) {
            return;
        }
        // Check general bucket use
        checkIsland(e, e.getBlockClicked().getLocation(), Flags.BUCKET);
    }

}
