package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
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

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

public class BreakBlocksListener extends FlagListener {

    /**
     * Prevents blocks from being broken
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.BREAK_BLOCKS);
    }

    /**
     * Prevents the breakage of hanging items
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player) {
            checkIsland(e, (Player)e.getRemover(), e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
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
            BlockIterator iterator = new BlockIterator(e.getPlayer(), 10);
            while (iterator.hasNext()) {
                Block lastBlock = iterator.next();
                if (lastBlock.getType().toString().endsWith("_SKULL") || (lastBlock.getType().toString().endsWith("_HEAD") && !lastBlock.getType().equals(Material.PISTON_HEAD))) {
                    checkIsland(e, e.getPlayer(), lastBlock.getLocation(), Flags.BREAK_BLOCKS);
                    return;
                }
            }
        } catch (Exception ignored) {
            // We can ignore this exception
        }

        switch (e.getClickedBlock().getType()) {
        case CAKE:
        case SPAWNER:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
            break;
        case DRAGON_EGG:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.DRAGON_EGG);
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
            checkIsland(e, (Player)e.getAttacker(), e.getVehicle().getLocation(), Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Protect item frames, armor stands, etc. Entities that are actually blocks...
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        // Only handle item frames, armor stands and end crystals
        if (!(e.getEntity() instanceof ItemFrame)
                && !(e.getEntity() instanceof ArmorStand)
                && !(e.getEntity() instanceof EnderCrystal)) {
            return;
        }
        // Get the attacker
        if (e.getDamager() instanceof Player) {
            checkIsland(e, (Player)e.getDamager(), e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
        } else if (e.getDamager() instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) e.getDamager();
            if (p.getShooter() instanceof Player && !checkIsland(e, (Player)p.getShooter(), e.getEntity().getLocation(), Flags.BREAK_BLOCKS)) {
                e.getEntity().setFireTicks(0);
                e.getDamager().remove();
            }
        }
    }
}
