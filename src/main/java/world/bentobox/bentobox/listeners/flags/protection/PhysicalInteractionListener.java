package world.bentobox.bentobox.listeners.flags.protection;

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
        switch (e.getClickedBlock().getType()) {
        case FARMLAND:
            // Crop trample
            checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
            break;

        case ACACIA_PRESSURE_PLATE:
        case BIRCH_PRESSURE_PLATE:
        case DARK_OAK_PRESSURE_PLATE:
        case HEAVY_WEIGHTED_PRESSURE_PLATE:
        case JUNGLE_PRESSURE_PLATE:
        case LIGHT_WEIGHTED_PRESSURE_PLATE:
        case OAK_PRESSURE_PLATE:
        case SPRUCE_PRESSURE_PLATE:
        case STONE_PRESSURE_PLATE:
            // Pressure plates
            checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.PRESSURE_PLATE);
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
        if (p.getShooter() instanceof Player && e.getBlock() != null) {

            switch(e.getBlock().getType()) {
            case ACACIA_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case STONE_BUTTON:
            case DARK_OAK_BUTTON:
                checkIsland(e, (Player)p.getShooter(), e.getBlock().getLocation(), Flags.BUTTON);
                break;
            case ACACIA_PRESSURE_PLATE:
            case BIRCH_PRESSURE_PLATE:
            case DARK_OAK_PRESSURE_PLATE:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case JUNGLE_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE:
            case SPRUCE_PRESSURE_PLATE:
            case STONE_PRESSURE_PLATE:
                // Pressure plates
                checkIsland(e, (Player)p.getShooter(), e.getBlock().getLocation(), Flags.PRESSURE_PLATE);
                break;
            default:
                break;
            }
        }
    }

}
