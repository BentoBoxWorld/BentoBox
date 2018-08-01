package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.util.BlockIterator;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

public class BreakBlocksListener extends AbstractFlagListener {

    /**
     * Prevents blocks from being broken
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        checkIsland(e, e.getBlock().getLocation(), Flags.BREAK_BLOCKS);
    }

    /**
     * Prevents the breakage of hanging items
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player) {
            setUser(User.getInstance(e.getRemover())).checkIsland(e, e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Handles breaking objects
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        // Only handle hitting things
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // Look along player's sight line to see if any blocks are skulls
        try {
            BlockIterator iter = new BlockIterator(e.getPlayer(), 10);
            Block lastBlock = iter.next();
            while (iter.hasNext()) {
                lastBlock = iter.next();
                if (lastBlock.getType().toString().endsWith("_SKULL") || (lastBlock.getType().toString().endsWith("_HEAD") && !lastBlock.getType().equals(Material.PISTON_HEAD))) {
                    checkIsland(e, lastBlock.getLocation(), Flags.BREAK_BLOCKS);
                    return;
                }
            }
        } catch (Exception ignored) {}

        switch (e.getClickedBlock().getType()) {
        case CAKE:
        case DRAGON_EGG:
        case SPAWNER:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
            break;
        default:
            break;
        }
    }

    /**
     * Handles vehicle breaking
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamageEvent(VehicleDamageEvent e) {
        if (getIWM().inWorld(e.getVehicle().getLocation()) && e.getAttacker() instanceof Player) {
            setUser(User.getInstance((Player) e.getAttacker()));
            checkIsland(e, e.getVehicle().getLocation(), Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Protect item frames, armor stands, etc. Entities that are actually blocks...
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        // Only handle item frames and armor stands
        if (!(e.getEntity() instanceof ItemFrame) && !(e.getEntity() instanceof ArmorStand)) {
            return;
        }

        // Get the attacker
        if (e.getDamager() instanceof Player) {
            setUser(User.getInstance(e.getDamager())).checkIsland(e, e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
        } else if (e.getDamager() instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) e.getDamager();
            if (p.getShooter() instanceof Player && !setUser(User.getInstance((Player)p.getShooter())).checkIsland(e, e.getEntity().getLocation(), Flags.BREAK_BLOCKS)) {
                e.getEntity().setFireTicks(0);
                e.getDamager().remove();
            }
        }
    }
}
