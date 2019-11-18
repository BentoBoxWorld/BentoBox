package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.TropicalFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles interaction with beds
 * Note - bed protection from breaking or placing is done elsewhere.
 * @author tastybento
 *
 */
public class BucketListener extends FlagListener {

    /**
     * Prevents emptying of buckets
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        // This is where the water or lava actually will be dumped
        Block dumpBlock = e.getBlockClicked().getRelative(e.getBlockFace());
        checkIsland(e, e.getPlayer(), dumpBlock.getLocation(), Flags.BUCKET);
    }

    /**
     * Prevents collecting of lava, water, milk. If bucket use is denied in general, it is blocked.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
        // Check filling of various liquids
        if (e.getItemStack().getType().equals(Material.LAVA_BUCKET) && (!checkIsland(e, e.getPlayer(), e.getBlockClicked().getLocation(), Flags.COLLECT_LAVA))) {
            return;
        }
        if (e.getItemStack().getType().equals(Material.WATER_BUCKET) && (!checkIsland(e, e.getPlayer(), e.getBlockClicked().getLocation(), Flags.COLLECT_WATER))) {
            return;
        }
        if (e.getItemStack().getType().equals(Material.MILK_BUCKET) && (!checkIsland(e, e.getPlayer(), e.getBlockClicked().getLocation(), Flags.MILKING))) {
            return;
        }
        // Check general bucket use
        checkIsland(e, e.getPlayer(), e.getBlockClicked().getLocation(), Flags.BUCKET);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTropicalFishScooping(final PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof TropicalFish && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.WATER_BUCKET)) {
            checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.FISH_SCOOPING);
        }
    }


    /**
     * Prevents collecting mushroom strew from MushroomCow if player does not have access to Milking flag.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBowlFill(final PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof MushroomCow && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BOWL)) {
            checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.MILKING);
        }
    }
}