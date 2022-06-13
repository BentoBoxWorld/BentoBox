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
public class PhysicalInteractionListener extends FlagListener
{
    /**
     * Handle physical interaction with blocks
     * Crop trample, pressure plates, triggering redstone, tripwires
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if (!e.getAction().equals(Action.PHYSICAL) || e.getClickedBlock() == null)
        {
            return;
        }

        if (Tag.PRESSURE_PLATES.isTagged(e.getClickedBlock().getType()))
        {
            // Pressure plates
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.PRESSURE_PLATE);
            return;
        }

        switch (e.getClickedBlock().getType())
        {
            case FARMLAND -> this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
            case TURTLE_EGG -> this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.TURTLE_EGGS);
        }
    }


    /**
     * Protects buttons and plates from being activated by projectiles
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(EntityInteractEvent e)
    {
        if (!(e.getEntity() instanceof Projectile p))
        {
            return;
        }

        if (p.getShooter() instanceof Player)
        {
            if (Tag.WOODEN_BUTTONS.isTagged(e.getBlock().getType()))
            {
                this.checkIsland(e, (Player) p.getShooter(), e.getBlock().getLocation(), Flags.BUTTON);
                return;
            }

            if (Tag.PRESSURE_PLATES.isTagged(e.getBlock().getType()))
            {
                // Pressure plates
                this.checkIsland(e, (Player) p.getShooter(), e.getBlock().getLocation(), Flags.PRESSURE_PLATE);
            }
        }
    }
}
