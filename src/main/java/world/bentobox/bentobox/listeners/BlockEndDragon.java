package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
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
        testLocation(event.getPlayer().getLocation());
    }

    private void testLocation(Location location) {
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
        testLocation(event.getPlayer().getLocation());
    }

    /**
     * Silently prevents block placing in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockPlace(BlockPlaceEvent e) {
        e.setCancelled(testBlock(e.getBlock()));
    }

    private boolean testBlock(Block block) {
        return block.getY() == 255
                && block.getX() == 0
                && block.getZ() == 0
                && block.getWorld().getEnvironment().equals(Environment.THE_END)
                && Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(block.getWorld())
                && plugin.getIWM().inWorld(block.getWorld())
                && plugin.getIWM().isEndGenerate(block.getWorld())
                && plugin.getIWM().isEndIslands(block.getWorld());
    }

    /**
     * Silently prevents block breaking in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockBreak(BlockBreakEvent e) {
        e.setCancelled(testBlock(e.getBlock()));
    }
}
