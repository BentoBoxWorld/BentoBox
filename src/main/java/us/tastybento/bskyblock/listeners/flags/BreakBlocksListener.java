package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.Material;
import org.bukkit.World.Environment;
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

import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.lists.Flags;

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
                if (lastBlock.getType().equals(Material.SKULL)) {
                    checkIsland(e, lastBlock.getLocation(), Flags.BREAK_BLOCKS);
                    return;
                }
            }
        } catch (Exception ex) {}

        switch (e.getClickedBlock().getType()) {
        case CAKE_BLOCK:
        case DRAGON_EGG:
        case MOB_SPAWNER:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
            return;
        case BED_BLOCK:
            if (e.getPlayer().getWorld().getEnvironment().equals(Environment.NETHER)) {
                // Prevent explosions checkIsland(e, e.getClickedBlock().getLocation(), Flags.BREAK_BLOCKS);
                return;
            }
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
        if (getIslandWorldManager().inWorld(e.getVehicle().getLocation()) && e.getAttacker() instanceof Player) {
            User user = User.getInstance((Player) e.getAttacker());
            // Get the island and if present, check the flag, react if required and return
            getIslands().getIslandAt(e.getVehicle().getLocation()).ifPresent(x -> {
                if (!x.isAllowed(user, Flags.BREAK_BLOCKS)) {
                    e.setCancelled(true);
                    user.sendMessage("protection.protected");
                }
                return;
            });

            // The player is in the world, but not on an island, so general world settings apply
            if (!Flags.BREAK_BLOCKS.isDefaultSetting()) {
                e.setCancelled(true);
                user.sendMessage("protection.protected");
            }
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
            if (p.getShooter() instanceof Player) {
                if (!setUser(User.getInstance((Player)p.getShooter())).checkIsland(e, e.getEntity().getLocation(), Flags.BREAK_BLOCKS)) {
                    e.getEntity().setFireTicks(0);
                    e.getDamager().remove();
                }
            }
        }
    }

}
