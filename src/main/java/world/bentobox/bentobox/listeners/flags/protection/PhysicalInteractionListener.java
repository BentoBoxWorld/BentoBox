package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class PhysicalInteractionListener extends FlagListener {

    /**
     * Handle physical interaction with blocks
     * Crop trample, pressure plates, triggering redstone, tripwires
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL)) {
            return;
        }
        if (isPressurePlate(e.getClickedBlock().getType())) {
            // Pressure plates
            checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.PRESSURE_PLATE);
            return;
        }
        switch (e.getClickedBlock().getType()) {
        case FARMLAND:
            // Crop trample
            checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
            break;
        case TURTLE_EGG:
            checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.TURTLE_EGGS);
            break;
        default:
            break;
        }
    }

    /**
     * Protects buttons and plates from being activated by projectiles
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(EntityInteractEvent e) {
        if (!(e.getEntity() instanceof Projectile)) {
            return;
        }
        Projectile p = (Projectile)e.getEntity();
        if (p.getShooter() instanceof Player) {
            if (Tag.WOODEN_BUTTONS.isTagged(e.getBlock().getType())) {
                checkIsland(e, (Player)p.getShooter(), e.getBlock().getLocation(), Flags.BUTTON);
                return;
            }

            if (isPressurePlate(e.getBlock().getType())) {
                // Pressure plates
                checkIsland(e, (Player)p.getShooter(), e.getBlock().getLocation(), Flags.PRESSURE_PLATE);
            }
        }
    }

    private boolean isPressurePlate(Material material) {
        return switch (material) {
            case STONE_PRESSURE_PLATE, POLISHED_BLACKSTONE_PRESSURE_PLATE, ACACIA_PRESSURE_PLATE, BIRCH_PRESSURE_PLATE, CRIMSON_PRESSURE_PLATE, DARK_OAK_PRESSURE_PLATE, HEAVY_WEIGHTED_PRESSURE_PLATE, JUNGLE_PRESSURE_PLATE, LIGHT_WEIGHTED_PRESSURE_PLATE, OAK_PRESSURE_PLATE, SPRUCE_PRESSURE_PLATE, WARPED_PRESSURE_PLATE -> true;
            default -> false;
        };
    }

}
