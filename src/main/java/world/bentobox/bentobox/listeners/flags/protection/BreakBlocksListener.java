package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

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
        Player p = e.getPlayer();
        Location l = e.getBlock().getLocation();
        Material m = e.getBlock().getType();
        if (m.equals(Material.MELON) || m.equals(Material.PUMPKIN)) {
            this.checkIsland(e, p, l, Flags.HARVEST);
        } else {
            // Crops
            if (Tag.CROPS.isTagged(m)
                    && !m.equals(Material.MELON_STEM)
                    && !m.equals(Material.PUMPKIN_STEM)
                    && !m.equals(Material.ATTACHED_MELON_STEM)
                    && !m.equals(Material.ATTACHED_PUMPKIN_STEM)) {
                this.checkIsland(e,  p,  l, Flags.HARVEST);
            } else {
                checkIsland(e, p, l, Flags.BREAK_BLOCKS);
            }
        }
    }

    /**
     * Prevents the breakage of hanging items
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player r) {
            checkIsland(e, r, e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
        }
        // Check for projectiles
        // Find out who fired it
        if (e.getRemover() instanceof Projectile p && p.getShooter() instanceof Player s) {
            checkIsland(e, s, e.getEntity().getLocation(), Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Handles breaking objects
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e)
    {
        // Only handle hitting things
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getClickedBlock() == null)
        {
            return;
        }
        Player p = e.getPlayer();
        Location l = e.getClickedBlock().getLocation();
        Material m = e.getClickedBlock().getType();
        switch (m)
        {
        case CAKE -> this.checkIsland(e, p, l, Flags.BREAK_BLOCKS);
        case SPAWNER -> this.checkIsland(e, p, l, Flags.BREAK_SPAWNERS);
        case DRAGON_EGG -> this.checkIsland(e, p, l, Flags.DRAGON_EGG);
        case HOPPER -> this.checkIsland(e, p, l, Flags.BREAK_HOPPERS);
        default -> {
            // Do nothing
        }
        }
    }

    /**
     * Handles vehicle breaking
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamageEvent(VehicleDamageEvent e)
    {
        Location l = e.getVehicle().getLocation();

        if (getIWM().inWorld(l) && e.getAttacker() instanceof Player p)
        {
            String vehicleType = e.getVehicle().getType().name();

            // 1.19 introduced Chest Boat.
            if (vehicleType.contains("BOAT"))
            {
                this.checkIsland(e, p, l, Flags.BOAT);
            }
            else if (vehicleType.contains("MINECART"))
            {
                this.checkIsland(e, p, l, Flags.MINECART);
            }
            else
            {
                this.checkIsland(e, p, l, Flags.BREAK_BLOCKS);
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
        if (e.getDamager() instanceof Player p) {
            // Check the break blocks flag
            notAllowed(e, p, e.getEntity().getLocation());
        } else if (e.getDamager() instanceof Projectile p && // Find out who fired the arrow
                p.getShooter() instanceof Player player && notAllowed(e, player, e.getEntity().getLocation())) {
            e.getEntity().setFireTicks(0);
            p.setFireTicks(0);
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
        if (e.getEntity().getShooter() instanceof Player s &&
                !checkIsland(e, s, e.getHitBlock().getLocation(), Flags.BREAK_BLOCKS)) {
            final BlockData data = e.getHitBlock().getBlockData();
            // We seemingly can't prevent the block from being destroyed
            // So we need to put it back with a slight delay (yup, this is hacky - it makes the block flicker sometimes)
            e.getHitBlock().setType(Material.AIR); // prevents the block from dropping a chorus flower
            getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> e.getHitBlock().setBlockData(data, true));
            // Sorry, this might also cause some ghost blocks!
        }
    }
}
