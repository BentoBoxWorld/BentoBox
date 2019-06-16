package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;

public class BlockEndDragon implements Listener {

    private BentoBox plugin;

    public BlockEndDragon(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a portal frame at the top of the world, when a player joins an island End world.
     * This prevents the Ender Dragon from spawning: if any portal frame exists, then the dragon is considered killed already.
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Location location = event.getPlayer().getLocation();

        if (!plugin.getIWM().isIslandEnd(location.getWorld())
            || !Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(location.getWorld())
            || location.getWorld().getBlockAt(0, 255, 0).getType().equals(Material.END_PORTAL)) {
            return;
        }

        // Setting a End Portal at the top will trick dragon legacy check.
        location.getWorld().getBlockAt(0, 255, 0).setType(Material.END_PORTAL, false);
    }

    /**
     * Adds a portal frame at the top of the world, when a player joins an island End world.
     * This prevents the Ender Dragon from spawning: if any portal frame exists, then the dragon is considered killed already.
     * @param event PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerJoinWorld(PlayerJoinEvent event) {
        Location location = event.getPlayer().getLocation();

        if (!plugin.getIWM().isIslandEnd(location.getWorld())
            || !Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(location.getWorld())
            || location.getWorld().getBlockAt(0, 255, 0).getType().equals(Material.END_PORTAL)) {
            return;
        }

        // Setting a End Portal at the top will trick dragon legacy check.
        location.getWorld().getBlockAt(0, 255, 0).setType(Material.END_PORTAL, false);
    }

    /**
     * Silently prevents block placing in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getY() != 255
                || e.getBlock().getX() != 0
                || e.getBlock().getZ() != 0
                || !e.getBlock().getType().equals(Material.END_PORTAL)
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
                || !Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getBlock().getWorld())
                || !plugin.getIWM().inWorld(e.getBlock().getWorld())
                || !plugin.getIWM().isEndGenerate(e.getBlock().getWorld())
                || !plugin.getIWM().isEndIslands(e.getBlock().getWorld())) {
            return;
        }
        e.setCancelled(true);
    }

    /**
     * Silently prevents block breaking in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getY() != 255
                || e.getBlock().getX() != 0
                || e.getBlock().getZ() != 0
                || !e.getBlock().getType().equals(Material.END_PORTAL)
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
                || !Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getBlock().getWorld())
                || !plugin.getIWM().inWorld(e.getBlock().getWorld())
                || !plugin.getIWM().isEndGenerate(e.getBlock().getWorld())
                || !plugin.getIWM().isEndIslands(e.getBlock().getWorld())) {
            return;
        }
        e.setCancelled(true);
    }
}
