package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.util.BlockIterator;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles fire
 * @author tastybento
 *
 */
public class FireListener extends FlagListener {

    /**
     * Checks if fire is allowed. If not, cancels the action
     * @param e - cancellable event
     * @param l - location
     * @param flag - flag to check
     * @return - true if cancelled, false if not
     */
    public boolean checkFire(Cancellable e, Location l, Flag flag) {
        // Check world
        if (!getIWM().inWorld(l)) {
            return false;
        }
        // Check if the island exists and if fire is allowed
        boolean cancel = getIslands().getIslandAt(l).map(i -> !i.isAllowed(flag)).orElseGet(() -> !flag.isSetForWorld(l.getWorld()));
        e.setCancelled(cancel);
        return cancel;
    }

    /**
     * Prevents fire spread
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        checkFire(e, e.getBlock().getLocation(), Flags.FIRE_BURNING);
    }

    /**
     * Prevent fire spread
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (e.getSource().getType().equals(Material.FIRE)) {
            checkFire(e, e.getBlock().getLocation(), Flags.FIRE_SPREAD);
        }
    }

    /**
     * Igniting fires
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        // Check if this is a portal lighting - that is allowed any time
        if (!e.getBlock().getType().equals(Material.OBSIDIAN)) {
            checkFire(e, e.getBlock().getLocation(), Flags.FIRE_IGNITE);
        }
    }

    /**
     * Flint and Steel and Extinguishing fire
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && (e.getMaterial() == Material.FLINT_AND_STEEL || e.getMaterial() == Material.FIRE_CHARGE)) {
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.FLINT_AND_STEEL);
        }
        // Look along player's sight line to see if any blocks are fire. Players can hit fire out quite a long way away.
        try {
            BlockIterator iter = new BlockIterator(e.getPlayer(), 10);
            while (iter.hasNext()) {
                Block lastBlock = iter.next();
                if (lastBlock.getType().equals(Material.FIRE)) {
                    checkIsland(e, e.getPlayer(), lastBlock.getLocation(), Flags.FIRE_EXTINGUISH);
                }
            }
        } catch (Exception ex) {
            // To catch at block iterator exceptions that can happen in the void or at the very top of blocks
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLightningStrikeEvent(LightningStrikeEvent e) {
        checkFire(e, e.getLightning().getLocation(), Flags.FIRE_IGNITE);
    }
}
