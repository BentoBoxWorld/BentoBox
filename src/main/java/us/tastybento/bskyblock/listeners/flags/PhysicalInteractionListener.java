package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import us.tastybento.bskyblock.api.flags.AbstractFlagListener;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author tastybento
 *
 */
public class PhysicalInteractionListener extends AbstractFlagListener {

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
        case SOIL:
            // Crop trample
            checkIsland(e, e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
            break;
        case WOOD_PLATE:
        case STONE_PLATE:
        case GOLD_PLATE:
        case IRON_PLATE:
            // Pressure plates
            checkIsland(e, e.getPlayer().getLocation(), Flags.PRESSURE_PLATE);
            break;
        default:
            break;

        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(EntityInteractEvent e) {
        if (e.getEntity() == null || !(e.getEntity() instanceof Projectile)) {
            return;
        }
        Projectile p = (Projectile)e.getEntity();
        if (p.getShooter() != null && p.getShooter() instanceof Player && e.getBlock() != null) {
            // Set the user to the shooter
            setUser(User.getInstance((Player)p.getShooter()));

            switch(e.getBlock().getType()) {
            case WOOD_BUTTON:
            case STONE_BUTTON:
                checkIsland(e, e.getBlock().getLocation(), Flags.BUTTON);
                break;
            case WOOD_PLATE:
            case STONE_PLATE:
            case GOLD_PLATE:
            case IRON_PLATE:
                // Pressure plates
                checkIsland(e, e.getBlock().getLocation(), Flags.PRESSURE_PLATE);
                break;
            default:
                break;
            }
        }
    }

}
