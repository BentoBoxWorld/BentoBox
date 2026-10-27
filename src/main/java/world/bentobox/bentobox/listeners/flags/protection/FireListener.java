package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
        // Lighting a portal is allowed any time, but only a real obsidian frame is exempt:
        // fire on any obsidian floor must still respect FIRE_IGNITE.
        // Only player-caused ignition can light a portal, so the expensive frame scan is skipped
        // for spread/lava/explosion, which fire this event hundreds of times per second.
        BlockIgniteEvent.IgniteCause cause = e.getCause();
        boolean playerIgnition = cause == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                || cause == BlockIgniteEvent.IgniteCause.FIREBALL;
        if (playerIgnition && isPortalFrame(e.getBlock())) {
            return;
        }
        checkFire(e, e.getBlock().getLocation(), Flags.FIRE_IGNITE);
    }

    /**
     * Flint and Steel and Extinguishing fire
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && (e.getMaterial() == Material.FLINT_AND_STEEL || e.getMaterial() == Material.FIRE_CHARGE)) {
            Block clicked = e.getClickedBlock();
            // Only a real obsidian frame may be lit without the flag check.
            if (clicked == null || !isPortalFrame(clicked.getRelative(e.getBlockFace()))) {
                checkIsland(e, e.getPlayer(), clicked == null ? e.getPlayer().getLocation() : clicked.getLocation(),
                        Flags.FLINT_AND_STEEL);
            }
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

    /**
     * Checks whether the block is the bottom of a nether portal interior: at least two columns
     * wide and three high, enclosed by obsidian. Any other obsidian construction is not exempt.
     *
     * @param fire block where the fire would be placed
     * @return true if it is a portal frame
     */
    static boolean isPortalFrame(Block fire) {
        // The interior can be 3 to 21 blocks high, so find the roof instead of assuming 3.
        int height = interiorHeight(fire);
        if (height < 3) {
            return false;
        }
        for (BlockFace axis : new BlockFace[] {BlockFace.EAST, BlockFace.NORTH}) {
            int left = interiorWidth(fire, axis.getOppositeFace());
            int right = interiorWidth(fire, axis);
            // A portal interior is at least two columns wide and at most 21.
            if (left < 0 || right < 0 || left + right + 1 < 2 || left + right + 1 > 21) {
                continue;
            }
            Block leftFrame = fire.getRelative(axis.getOppositeFace(), left + 1);
            Block rightFrame = fire.getRelative(axis, right + 1);
            boolean framed = true;
            for (int y = 0; y <= height && framed; y++) {
                framed = leftFrame.getRelative(BlockFace.UP, y).getType() == Material.OBSIDIAN
                        && rightFrame.getRelative(BlockFace.UP, y).getType() == Material.OBSIDIAN;
            }
            // Every interior column needs an obsidian floor and an obsidian roof.
            for (int x = -left; x <= right && framed; x++) {
                Block column = fire.getRelative(axis, x);
                framed = column.getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN
                        && column.getRelative(BlockFace.UP, height).getType() == Material.OBSIDIAN;
            }
            if (framed) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param fire block where the fire would be placed
     * @return height of the interior including the fire block, or -1 if there is no obsidian roof
     */
    private static int interiorHeight(Block fire) {
        for (int i = 1; i <= 21; i++) {
            Material type = fire.getRelative(BlockFace.UP, i).getType();
            if (type == Material.OBSIDIAN) {
                return i;
            }
            if (type.isSolid()) {
                return -1;
            }
        }
        return -1;
    }

    /** @return blocks between the fire and the obsidian frame, or -1 if there is no frame */
    private static int interiorWidth(Block fire, BlockFace face) {
        for (int i = 1; i <= 21; i++) {
            Material type = fire.getRelative(face, i).getType();
            if (type == Material.OBSIDIAN) {
                return i - 1;
            }
            if (!type.isAir() && type != Material.FIRE) {
                return -1;
            }
        }
        return -1;
    }
}
