package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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
        // Check for projectiles
        if (e.getRemover() instanceof Projectile) {
            // Find out who fired it
            Projectile p = (Projectile)e.getRemover();
            if (p.getShooter() instanceof Player) {
                checkIsland(e, (Player)p.getShooter(), e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
            }
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
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
            break;
        case SPAWNER:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREAK_SPAWNERS);
            break;
        case DRAGON_EGG:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.DRAGON_EGG);
            break;
        case HOPPER:
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREAK_HOPPERS);
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
            String vehicleType = e.getVehicle().getType().toString();
            if (e.getVehicle().getType().equals(EntityType.BOAT)) {
                checkIsland(e, (Player) e.getAttacker(), e.getVehicle().getLocation(), Flags.BOAT);
            } else if (vehicleType.contains("MINECART")) {
                checkIsland(e, (Player) e.getAttacker(), e.getVehicle().getLocation(), Flags.MINECART);
            } else {
                checkIsland(e, (Player) e.getAttacker(), e.getVehicle().getLocation(), Flags.BREAK_BLOCKS);
            }
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
            // Check the break blocks flag
            notAllowed(e, (Player)e.getDamager(), e.getEntity().getLocation());
        } else if (e.getDamager() instanceof Projectile) {
            // Find out who fired the arrow
            Projectile p = (Projectile) e.getDamager();
            if (p.getShooter() instanceof Player && notAllowed(e, (Player)p.getShooter(), e.getEntity().getLocation())) {
                e.getEntity().setFireTicks(0);
                p.setFireTicks(0);
            }
        }
    }

    private boolean notAllowed(EntityDamageByEntityEvent e, Player player, Location location) {
        if (!checkIsland(e, player, location, Flags.BREAK_BLOCKS)) return true;
        if (e.getEntity() instanceof ItemFrame) {
            return !checkIsland(e, player, location, Flags.ITEM_FRAME);
        } else if (e.getEntity() instanceof ArmorStand) {
            return !checkIsland(e, player, location, Flags.ARMOR_STAND);
        }
        return false;
    }

    /**
     * Prevents Chorus Flowers from being broken by an arrow or a trident
     * @param e event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileHitBreakBlock(ProjectileHitEvent e) {
        // We want to make sure this is an actual projectile (arrow or trident)
        if (!(e.getEntity() instanceof AbstractArrow)) {
            return;
        }

        // We want to make sure it hit a CHORUS_FLOWER
        if (e.getHitBlock() == null || !e.getHitBlock().getType().equals(Material.CHORUS_FLOWER)) {
            return;
        }

        // Find out who fired the arrow
        if (e.getEntity().getShooter() instanceof Player &&
                !checkIsland(e, (Player) e.getEntity().getShooter(), e.getHitBlock().getLocation(), Flags.BREAK_BLOCKS)) {
            final BlockData data = e.getHitBlock().getBlockData();
            // We seemingly can't prevent the block from being destroyed
            // So we need to put it back with a slight delay (yup, this is hacky - it makes the block flicker sometimes)
            e.getHitBlock().setType(Material.AIR); // prevents the block from dropping a chorus flower
            getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> e.getHitBlock().setBlockData(data, true));
            // Sorry, this might also cause some ghost blocks!
        }
    }
}
