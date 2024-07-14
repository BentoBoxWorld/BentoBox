package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;


/**
 * Listener for {@link Flags#CROP_TRAMPLE}, {@link Flags#PRESSURE_PLATE}, {@link Flags#TURTLE_EGGS}, {@link Flags#BUTTON}
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
        if (e.getClickedBlock().getType() == Material.FARMLAND) {
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
        } else if (e.getClickedBlock().getType() == Material.TURTLE_EGG) {
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.TURTLE_EGGS);
        }
    }


    /**
     * Protects buttons and plates from being activated by projectiles
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(EntityInteractEvent e)
    {
        if (e.getEntity() instanceof Projectile p && p.getShooter() instanceof Player player)
        {
            checkBlocks(e, player, e.getBlock());
        }
    }

    private void checkBlocks(Event e, Player player, Block block) {
        if (Tag.WOODEN_BUTTONS.isTagged(block.getType())) {
            this.checkIsland(e, player, block.getLocation(), Flags.BUTTON);
            return;
        }

        if (Tag.PRESSURE_PLATES.isTagged(block.getType())) {
            // Pressure plates
            this.checkIsland(e, player, block.getLocation(), Flags.PRESSURE_PLATE);
        }

    }

    /**
     * Protects buttons and plates from being activated by projectiles that explode
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileExplode(EntityExplodeEvent e) {
        if (e.getEntity() instanceof Projectile p && p.getShooter() instanceof Player player) {
            for (Block b : e.blockList()) {
                this.checkBlocks(e, player, b);
            }
        }
    }


}
