package world.bentobox.bentobox.managers.island;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * Provides island locations and pregenerates future chunks to speed up island teleportation
 * @author tastybento
 * @since 1.17.0
 *
 */
public class PregenNewIslandLocationStrategy implements NewIslandLocationStrategy {

    private static final long TIME = 40;
    private static final int VD = 16 * Bukkit.getServer().getViewDistance();
    protected BentoBox plugin = BentoBox.getInstance();
    protected Queue<Location> queue = new LinkedList<>();
    protected boolean loading;
    private BukkitTask task;

    @Override
    public Location getNextLocation(World world) {
        Location last = plugin.getIslands().getLast(world);
        if (last == null) {
            last = new Location(world,
                    (double) plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world),
                    plugin.getIWM().getIslandHeight(world),
                    (double) plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world));
        }
        // Find a free spot
        while (isIsland(last)) {
            nextGridLocation(last);
        }        
        plugin.getIslands().setLast(last);
        // Make a queue to load
        Location loc = last.clone();
        for (int i = 0; i < 5; i++) {
            nextGridLocation(loc);
            if (!Util.isChunkGenerated(loc)) {
                for (int x = loc.getBlockX() - VD; x <= loc.getBlockX() + VD; x += 16) {
                    for (int z = loc.getBlockZ() - VD; z <= loc.getBlockZ() + VD; z += 16) {
                        queue.add(new Location(loc.getWorld(), x, loc.getBlockY(), z));
                    }  
                }
                queue.add(loc.clone());
            }
        }
        // Load chunks
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!loading && !queue.isEmpty()) {
                Location l = queue.poll();
                if (Util.isChunkGenerated(l)) {
                    loading = true;
                    BentoBox.getInstance().logDebug("Generating chunk at " + l);
                    Util.getChunkAtAsync(l).thenRun(() -> loading = false);
                }
            }
            if (queue.isEmpty()) {
                task.cancel();
            }
        }, TIME, TIME);
        return last;
    }



    /**
     * Checks if there is an island or blocks at this location
     *
     * @param location - the location
     * @return true if island known
     */
    protected boolean isIsland(Location location) {
        return plugin.getIslands().getIslandAt(location).isPresent();
    }

    /**
     * Finds the next free island spot based off the last known island.
     * Uses island_distance setting from the config file. Builds up in a grid fashion.
     *
     * @param lastIsland - last island location
     */
    private void nextGridLocation(final Location lastIsland) {
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        int d = plugin.getIWM().getIslandDistance(lastIsland.getWorld()) * 2;
        if (x < z) {
            if (-1 * x < z) {
                lastIsland.setX(lastIsland.getX() + d);
                return;
            }
            lastIsland.setZ(lastIsland.getZ() + d);
            return;
        }
        if (x > z) {
            if (-1 * x >= z) {
                lastIsland.setX(lastIsland.getX() - d);
                return;
            }
            lastIsland.setZ(lastIsland.getZ() - d);
            return;
        }
        if (x <= 0) {
            lastIsland.setZ(lastIsland.getZ() + d);
            return;
        }
        lastIsland.setZ(lastIsland.getZ() - d);
        return;
    }
}
