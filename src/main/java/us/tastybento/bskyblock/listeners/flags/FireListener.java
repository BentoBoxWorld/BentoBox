/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockIterator;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles fire
 * @author tastybento
 *
 */
public class FireListener extends AbstractFlagListener {

    public FireListener(BSkyBlock plugin) {
        super(plugin);
    }

    /**
     * Prevents fire spread
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (!inWorld(e.getBlock().getLocation())) {
            return;
        }
        // Check if the island exists and if fire is allowed
        Optional<Island> island = plugin.getIslands().getIslandAt(e.getBlock().getLocation());
        island.ifPresent(x ->  {
            if (!x.isAllowed(Flags.FIRE_SPREAD)) e.setCancelled(true);
        });
        // If not on an island, check the default setting
        if (!island.isPresent() && !isDefaultAllowed(Flags.FIRE_SPREAD)) e.setCancelled(true);
    }

    /**
     * Prevent fire spread
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (e.getSource().getType().equals(Material.FIRE)) {
            if (!inWorld(e.getBlock().getLocation())) {
                return;
            }
            // Check if the island exists and if fire is allowed
            Optional<Island> island = plugin.getIslands().getIslandAt(e.getBlock().getLocation());
            island.ifPresent(x ->  {
                if (!x.isAllowed(Flags.FIRE_SPREAD)) e.setCancelled(true);
            });
            // If not on an island, check the default setting
            if (!island.isPresent() && !isDefaultAllowed(Flags.FIRE_SPREAD)) e.setCancelled(true);
        }
    }

    /**
     * Igniting fires
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (!inWorld(e.getBlock().getLocation())) {
            return;
        }
        // Check if this is a portal lighting - that is allowed any time
        if (e.getBlock().getType().equals(Material.OBSIDIAN)) {
            return;
        }
        // Check if the island exists and if fire is allowed
        Optional<Island> island = plugin.getIslands().getIslandAt(e.getBlock().getLocation());
        island.ifPresent(x ->  {
            if (!x.isAllowed(Flags.FIRE)) e.setCancelled(true);
        });
        // If not on an island, check the default setting
        if (!island.isPresent() && !isDefaultAllowed(Flags.FIRE)) e.setCancelled(true);

    }

    /**
     * Flint & Steel and Extinguishing fire
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getMaterial() != null && e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.FIRE);
        }
        // Look along player's sight line to see if any blocks are fire. Players can hit fire out quite a long way away.
        try {
            BlockIterator iter = new BlockIterator(e.getPlayer(), 10);
            Block lastBlock = iter.next();
            while (iter.hasNext()) {
                lastBlock = iter.next();
                if (lastBlock.equals(e.getClickedBlock())) {
                    break;
                }
                if (lastBlock.getType().equals(Material.FIRE)) {
                    checkIsland(e, lastBlock.getLocation(), Flags.FIRE_EXTINGUISH);
                }
            }
        } catch (Exception ex) {
            // To catch at block iterator exceptions that can happen in the void or at the very top of blocks
        }
    }

    /**
     * Protect TNT.
     * Note that allowing TNT to explode is governed by the Break Blocks flag.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTNTPrimed(EntityChangeBlockEvent e) {
        // Check world
        if (!inWorld(e.getBlock().getLocation())) {
            return;
        }
        // Check for TNT
        if (!e.getBlock().getType().equals(Material.TNT)) {
            //plugin.getLogger().info("DEBUG: not tnt");
            return;
        }
        // Check if the island exists and if fire is allowed
        Optional<Island> island = plugin.getIslands().getIslandAt(e.getBlock().getLocation());
        island.ifPresent(x ->  {
            if (!x.isAllowed(Flags.FIRE)) e.setCancelled(true);
        });
        // If not on an island, check the default setting
        if (!island.isPresent() && !isDefaultAllowed(Flags.FIRE)) e.setCancelled(true);

        // If either of these canceled the event, return
        if (e.isCancelled()) return;

        // Stop TNT from being damaged if it is being caused by a visitor with a flaming arrow
        if (e.getEntity() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getEntity();
            // Find out who fired it
            if (projectile.getShooter() instanceof Player) {
                if (projectile.getFireTicks() > 0) {
                    Player shooter = (Player)projectile.getShooter();
                    if (setUser(User.getInstance(shooter)).checkIsland(e, e.getBlock().getLocation(), Flags.BREAK_BLOCKS)) {
                        // Remove the arrow
                        projectile.remove();
                    }
                }
            }
        }
    }

}
