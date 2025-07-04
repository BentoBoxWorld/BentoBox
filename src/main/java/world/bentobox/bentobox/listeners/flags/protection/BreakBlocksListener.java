package world.bentobox.bentobox.listeners.flags.protection;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.type.CaveVinesPlant;
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

import com.google.common.base.Enums;

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
            if ((Tag.CROPS.isTagged(m)
                    && !m.equals(Material.MELON_STEM)
                    && !m.equals(Material.PUMPKIN_STEM)
                    && !m.equals(Material.ATTACHED_MELON_STEM)
                    && !m.equals(Material.ATTACHED_PUMPKIN_STEM))
                    || m == Material.COCOA
                    || m == Material.SWEET_BERRY_BUSH
                    || m == Material.BAMBOO
                    || m == Material.NETHER_WART
                    || m == Material.CACTUS
                    ) {
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

    private static final Method BERRIES_CHECK;

    static {
        Method m = null;
        try {
            m = CaveVinesPlant.class.getMethod("hasBerries");
        } catch (NoSuchMethodException ignored) {
            try {
                m = CaveVinesPlant.class.getMethod("isBerries");
            } catch (NoSuchMethodException ignored2) {
            }
        }
        BERRIES_CHECK = m;
    }
    /**
     * Handles breaking objects
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e)
    {
        if (e.getClickedBlock() == null) {
            return;
        }
        Player p = e.getPlayer();
        Location l = e.getClickedBlock().getLocation();
        Material m = e.getClickedBlock().getType();
        // Right click handling
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material clickedType = e.getClickedBlock().getType();
            switch (clickedType) {
            case CAVE_VINES, CAVE_VINES_PLANT -> {
                try {
                    boolean hasBerries = (Boolean) BERRIES_CHECK
                            .invoke((CaveVinesPlant) e.getClickedBlock().getBlockData());
                    if (hasBerries) {
                        this.checkIsland(e, p, l, Flags.HARVEST);
                    }
                } catch (ReflectiveOperationException ex) {
                    getPlugin().logStacktrace(ex);
                }
            }
            case SWEET_BERRY_BUSH -> this.checkIsland(e, p, l, Flags.HARVEST);
            case ROOTED_DIRT -> this.checkIsland(e, p, l, Flags.BREAK_BLOCKS);
            default -> { // Do nothing
            }
            }
            return;
        }
        // Only handle hitting things
        if (!(e.getAction() == Action.LEFT_CLICK_BLOCK) || e.getClickedBlock() == null)
        {
            return;
        }
        if (Enums.getIfPresent(Material.class, "TRIAL_SPAWNER").isPresent() && m.equals(Material.TRIAL_SPAWNER)) {
            this.checkIsland(e, p, l, Flags.BREAK_SPAWNERS);
            return;
        }
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
        if (e.getHitBlock() == null) {
            return;
        }

        // Check if the hit block is a Chorus Flower or a Decorated Pot
        if(!(e.getHitBlock().getType().equals(Material.CHORUS_FLOWER) ||
                e.getHitBlock().getType().equals(Material.DECORATED_POT))) {
            return;
        }

        // Find out who fired the arrow
        if (e.getEntity().getShooter() instanceof Player s &&
                !checkIsland(e, s, e.getHitBlock().getLocation(), Flags.BREAK_BLOCKS)) {

            e.setCancelled(true); // Prevents the block from being destroyed
        }
    }
}
