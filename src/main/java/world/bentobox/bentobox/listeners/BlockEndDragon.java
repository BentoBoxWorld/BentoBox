package world.bentobox.bentobox.listeners;

import java.util.Arrays;
import java.util.List;

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
import world.bentobox.bentobox.util.Pair;

public class BlockEndDragon implements Listener {

    private static final List<Pair<Integer, Integer>> CHUNKS = Arrays.asList(
            new Pair<Integer, Integer>(0,0),
            new Pair<Integer, Integer>(-1,0),
            new Pair<Integer, Integer>(-1, -1),
            new Pair<Integer, Integer>(0, -1));

    private static final int DEAD_ZONE_Y = 250;

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
        // Setting a bedrock block here forces the spike to be placed as high as possible
        e.getChunk().getBlock(0, 255, 0).setType(Material.BEDROCK);
    }

    /**
     * Silently prevents block placing in the dead zone.
     * This is just a simple protection.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndBlockPlace(BlockPlaceEvent e) {
        if (!Flags.REMOVE_END_EXIT_ISLAND.isSetForWorld(e.getBlock().getWorld())
                || e.getBlock().getY() < DEAD_ZONE_Y
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
                || !plugin.getIWM().inWorld(e.getBlock().getWorld())
                || !plugin.getIWM().isEndGenerate(e.getBlock().getWorld())
                || !plugin.getIWM().isEndIslands(e.getBlock().getWorld())
                || !CHUNKS.contains(new Pair<Integer, Integer>(e.getBlock().getChunk().getX(), e.getBlock().getChunk().getZ()))
                ) {
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
                || e.getBlock().getY() < DEAD_ZONE_Y
                || !e.getBlock().getWorld().getEnvironment().equals(Environment.THE_END)
                || !plugin.getIWM().inWorld(e.getBlock().getWorld())
                || !plugin.getIWM().isEndGenerate(e.getBlock().getWorld())
                || !plugin.getIWM().isEndIslands(e.getBlock().getWorld())
                || !CHUNKS.contains(new Pair<Integer, Integer>(e.getBlock().getChunk().getX(), e.getBlock().getChunk().getZ()))
                ) {
            return;
        }
        e.setCancelled(true);
    }
}
