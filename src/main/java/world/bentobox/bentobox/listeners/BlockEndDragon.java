package world.bentobox.bentobox.listeners;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;


public class BlockEndDragon implements Listener {

    private BentoBox plugin;

    public BlockEndDragon(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * This handles end dragon spawning prevention. Does not kill dragon because that generates random portal placement
     *
     * @param e - event
     * @return true if dragon can spawn, false if not
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onDragonSpawn(CreatureSpawnEvent e) {
        if (!e.getEntityType().equals(EntityType.ENDER_DRAGON) || plugin.getIWM().isDragonSpawn(e.getEntity().getWorld())) {
            return true;
        }
        e.getEntity().setHealth(0);
        e.getEntity().remove();
        e.setCancelled(true);
        return false;
    }


    /**
     * This listener moves the end exit island high up in the sky
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnd(ChunkLoadEvent e) {
        if (!Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getWorld())
                || !e.getWorld().getEnvironment().equals(Environment.THE_END)
                || !plugin.getIWM().inWorld(e.getWorld())
                || !plugin.getIWM().isEndGenerate(e.getWorld())
                || !plugin.getIWM().isEndIslands(e.getWorld())
                || !(e.getChunk().getX() == 0 && e.getChunk().getZ() == 0)) {
            return;
        }
        // Setting a End Portal at the top will trick dragon legacy check.
        e.getChunk().getBlock(0, 255, 0).setType(Material.END_PORTAL);
    }

    /**
     * Silently prevents block placing in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockPlace(BlockPlaceEvent e) {
        if (!Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getBlock().getWorld())
                || e.getBlock().getY() != 255
                || e.getBlock().getX() != 0
                || e.getBlock().getZ() != 0
                || !e.getBlock().getType().equals(Material.END_PORTAL)
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
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
        if (!Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getBlock().getWorld())
                || e.getBlock().getY() != 255
                || e.getBlock().getX() != 0
                || e.getBlock().getZ() != 0
                || !e.getBlock().getType().equals(Material.END_PORTAL)
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
                || !plugin.getIWM().inWorld(e.getBlock().getWorld())
                || !plugin.getIWM().isEndGenerate(e.getBlock().getWorld())
                || !plugin.getIWM().isEndIslands(e.getBlock().getWorld())) {
            return;
        }
        e.setCancelled(true);
    }
}
