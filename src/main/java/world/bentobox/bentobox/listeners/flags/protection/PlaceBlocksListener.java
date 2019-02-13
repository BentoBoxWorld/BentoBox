package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
public class PlaceBlocksListener extends FlagListener {

    /**
     * Check blocks being placed in general
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (e.getBlock().getType().equals(Material.FIRE)) {
            return;
        }
        checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.PLACE_BLOCKS);
    }

    /**
     * Handles placing items into ItemFrames
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerHitItemFrame(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType().equals(EntityType.ITEM_FRAME)) {
            checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.PLACE_BLOCKS);
        }
    }

    /**
     * Handle placing of fireworks, mine carts, end crystals, doors, chests and boats on land
     * The doors and chests are related to an exploit.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        switch (e.getClickedBlock().getType()) {
        case FIREWORK_ROCKET:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
            return;
        case RAIL:
        case POWERED_RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
            if (e.getMaterial() != null && (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.CHEST_MINECART || e.getMaterial() == Material.HOPPER_MINECART
            || e.getMaterial() == Material.TNT_MINECART || e.getMaterial() == Material.FURNACE_MINECART)) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.MINECART);
            }
            return;
        default:
            // Check in-hand items
            if (e.getMaterial() != null) {
                if (e.getMaterial().equals(Material.FIREWORK_ROCKET)
                        || e.getMaterial().equals(Material.ARMOR_STAND)
                        || e.getMaterial().equals(Material.END_CRYSTAL)
                        //|| Tag.DOORS.isTagged(e.getMaterial())
                        || e.getMaterial().equals(Material.CHEST) || e.getMaterial().equals(Material.TRAPPED_CHEST)) {
                    checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.PLACE_BLOCKS);
                }
                else if (e.getMaterial().name().contains("BOAT")) {
                    checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.BOAT);
                }
            }
        }
    }

    /**
     * Handles Frost Walking on visitor's islands. This creates ice blocks, which is like placing blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockForm(EntityBlockFormEvent e) {
        if (e.getNewState().getType().equals(Material.FROSTED_ICE) && e.getEntity() instanceof Player) {
            checkIsland(e, (Player)e.getEntity(), e.getBlock().getLocation(), Flags.FROST_WALKER);
        }
    }

}
