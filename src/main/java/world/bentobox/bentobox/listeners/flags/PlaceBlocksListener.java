package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
public class PlaceBlocksListener extends AbstractFlagListener {

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
        checkIsland(e, e.getBlock().getLocation(), Flags.PLACE_BLOCKS);
    }

    /**
     * Handles placing items into ItemFrames
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType().equals(EntityType.ITEM_FRAME)) {
            checkIsland(e, e.getRightClicked().getLocation(), Flags.PLACE_BLOCKS);
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
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
            return;
        case RAIL:
        case POWERED_RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
            if (e.getMaterial() != null && (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.CHEST_MINECART || e.getMaterial() == Material.HOPPER_MINECART
            || e.getMaterial() == Material.TNT_MINECART || e.getMaterial() == Material.FURNACE_MINECART)) {
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
            }
            return;
        default:
            // Check in-hand items
            // This check protects against an exploit in 1.7.9 against cactus
            // and sugar cane and placing boats on non-liquids
            if (e.getMaterial() != null
                    && (e.getMaterial().equals(Material.END_CRYSTAL) || Tag.DOORS.isTagged(e.getMaterial())
                    || e.getMaterial().equals(Material.CHEST) || e.getMaterial().equals(Material.TRAPPED_CHEST)
                    || (e.getMaterial().name().contains("BOAT")
                            && !e.getClickedBlock().isLiquid()))) {
                checkIsland(e, e.getPlayer().getLocation(), Flags.PLACE_BLOCKS);
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
            setUser(User.getInstance((Player)e.getEntity()));
            checkIsland(e, e.getBlock().getLocation(), Flags.PLACE_BLOCKS);
        }
    }

}
