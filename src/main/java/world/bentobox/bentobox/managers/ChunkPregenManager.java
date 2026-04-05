package world.bentobox.bentobox.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.util.Util;

/**
 * Manages chunk pre-generation for island worlds.
 * <p>
 * Predicts where the next islands will be placed (using the spiral grid algorithm)
 * and pre-generates chunks around those locations asynchronously using Paper's
 * async chunk API. This reduces lag spikes when players create or reset islands.
 * <p>
 * The manager coordinates pre-generation across all registered game mode addons,
 * round-robining between worlds to ensure fairness and prevent server overload.
 *
 * @author tastybento
 * @since 3.14.0
 */
public class ChunkPregenManager implements Listener {

    private record ChunkCoord(World world, int chunkX, int chunkZ) {}

    private final BentoBox plugin;
    private final Map<World, Deque<ChunkCoord>> chunkQueues = new ConcurrentHashMap<>();
    private final List<World> activeWorlds = new ArrayList<>();
    private BukkitTask schedulerTask;
    private int roundRobinIndex;

    public ChunkPregenManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts pre-generation for all game mode addons when BentoBox is fully loaded.
     *
     * @param e the ready event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        if (!plugin.getSettings().isPregenEnabled()) {
            return;
        }
        plugin.getAddonsManager().getGameModeAddons().forEach(this::schedulePregen);
        startTask();
    }

    /**
     * Re-triggers pre-generation when a new island is created (prediction window shifts).
     *
     * @param e the island created event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreated(IslandCreatedEvent e) {
        if (!plugin.getSettings().isPregenEnabled()) {
            return;
        }
        World world = e.getIsland().getWorld();
        plugin.getIWM().getAddon(world).ifPresent(this::schedulePregen);
    }

    /**
     * Re-triggers pre-generation when an island is reset (prediction window shifts).
     *
     * @param e the island resetted event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandResetted(IslandResettedEvent e) {
        if (!plugin.getSettings().isPregenEnabled()) {
            return;
        }
        World world = e.getIsland().getWorld();
        plugin.getIWM().getAddon(world).ifPresent(this::schedulePregen);
    }

    /**
     * Computes the chunks that need pre-generating for the given addon and queues them.
     *
     * @param addon the game mode addon
     */
    public void schedulePregen(GameModeAddon addon) {
        if (!addon.isFixIslandCenter()) {
            // Non-grid addons can't be predicted
            return;
        }

        World overworld = addon.getOverWorld();
        if (overworld == null) {
            return;
        }

        int islandsAhead = addon.getPregenIslandsAhead();
        if (islandsAhead == -1) {
            islandsAhead = plugin.getSettings().getPregenIslandsAhead();
        }
        if (islandsAhead <= 0) {
            return;
        }

        int islandDistance = plugin.getIWM().getIslandDistance(overworld) * 2;
        Location last = plugin.getIslands().getLast(overworld);
        if (last == null) {
            last = new Location(overworld,
                    (double) plugin.getIWM().getIslandXOffset(overworld) + plugin.getIWM().getIslandStartX(overworld),
                    plugin.getIWM().getIslandHeight(overworld),
                    (double) plugin.getIWM().getIslandZOffset(overworld) + plugin.getIWM().getIslandStartZ(overworld));
        }

        List<Location> predicted = predictNextLocations(overworld, last, islandsAhead, islandDistance);

        // Determine which worlds need chunks
        List<World> worlds = new ArrayList<>();
        worlds.add(overworld);
        if (plugin.getIWM().isNetherGenerate(overworld) && plugin.getIWM().isNetherIslands(overworld)) {
            World nether = plugin.getIWM().getNetherWorld(overworld);
            if (nether != null) {
                worlds.add(nether);
            }
        }
        if (plugin.getIWM().isEndGenerate(overworld) && plugin.getIWM().isEndIslands(overworld)) {
            World end = plugin.getIWM().getEndWorld(overworld);
            if (end != null) {
                worlds.add(end);
            }
        }

        int viewDistance = Bukkit.getViewDistance();

        // Build chunk queue, interleaving dimensions for round-robin within each island
        Deque<ChunkCoord> queue = new ArrayDeque<>();
        for (Location center : predicted) {
            int centerChunkX = center.getBlockX() >> 4;
            int centerChunkZ = center.getBlockZ() >> 4;

            // Collect chunks per world
            List<List<ChunkCoord>> perWorld = new ArrayList<>();
            for (World w : worlds) {
                List<ChunkCoord> chunks = new ArrayList<>();
                for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                    for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                        int cx = centerChunkX + dx;
                        int cz = centerChunkZ + dz;
                        if (!Util.isChunkGenerated(w, cx, cz)) {
                            chunks.add(new ChunkCoord(w, cx, cz));
                        }
                    }
                }
                perWorld.add(chunks);
            }

            // Interleave: take one from each world in turn
            boolean hasMore = true;
            int idx = 0;
            while (hasMore) {
                hasMore = false;
                for (List<ChunkCoord> chunks : perWorld) {
                    if (idx < chunks.size()) {
                        queue.add(chunks.get(idx));
                        hasMore = true;
                    }
                }
                idx++;
            }
        }

        if (!queue.isEmpty()) {
            chunkQueues.put(overworld, queue);
            plugin.log("Chunk pre-generation: queued " + queue.size() + " chunks for " + overworld.getName()
                    + " (" + predicted.size() + " islands ahead, " + worlds.size() + " dimension(s))");
            updateActiveWorlds();
            startTask();
        }
    }

    /**
     * Predicts the next N island locations by walking the spiral grid.
     * This is a pure function — it does not modify any state.
     *
     * @param world the world
     * @param start the starting location (last known island position)
     * @param count number of locations to predict
     * @param islandDistance the distance between island centers (island distance * 2)
     * @return list of predicted island center locations
     */
    static List<Location> predictNextLocations(World world, Location start, int count, int islandDistance) {
        List<Location> locations = new ArrayList<>(count);
        // Clone to avoid mutating the original
        Location pos = start.clone();
        for (int i = 0; i < count; i++) {
            nextGridLocation(pos, islandDistance);
            locations.add(pos.clone());
        }
        return locations;
    }

    /**
     * Moves the location to the next position in the outward square spiral.
     * Same algorithm as {@code DefaultNewIslandLocationStrategy.nextGridLocation}.
     *
     * @param loc the location to modify in-place
     * @param d the step distance between island centers
     */
    private static void nextGridLocation(Location loc, int d) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if (x < z) {
            if (-x < z) {
                loc.setX(loc.getX() + d);
            } else {
                loc.setZ(loc.getZ() + d);
            }
        } else if (x > z) {
            if (-x >= z) {
                loc.setX(loc.getX() - d);
            } else {
                loc.setZ(loc.getZ() - d);
            }
        } else {
            if (x <= 0) {
                loc.setZ(loc.getZ() + d);
            } else {
                loc.setZ(loc.getZ() - d);
            }
        }
    }

    /**
     * Starts the repeating task if not already running.
     */
    private void startTask() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            return;
        }
        int tickInterval = Math.max(1, plugin.getSettings().getPregenTickInterval());
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, tickInterval, tickInterval);
    }

    /**
     * Processes one batch of chunk generation requests across all worlds.
     */
    private void tick() {
        if (activeWorlds.isEmpty()) {
            // Nothing to do, cancel task
            if (schedulerTask != null) {
                schedulerTask.cancel();
                schedulerTask = null;
            }
            return;
        }

        int chunksPerTick = plugin.getSettings().getPregenChunksPerTick();
        int dispatched = 0;
        int worldsChecked = 0;

        while (dispatched < chunksPerTick && worldsChecked < activeWorlds.size()) {
            if (roundRobinIndex >= activeWorlds.size()) {
                roundRobinIndex = 0;
            }

            World world = activeWorlds.get(roundRobinIndex);
            Deque<ChunkCoord> queue = chunkQueues.get(world);

            if (queue == null || queue.isEmpty()) {
                chunkQueues.remove(world);
                activeWorlds.remove(roundRobinIndex);
                if (activeWorlds.isEmpty()) {
                    plugin.log("Chunk pre-generation: all queues complete");
                    break;
                }
                // Don't increment index since list shifted
                continue;
            }

            // Dispatch a fair share for this world
            int batchSize = Math.max(1, chunksPerTick / activeWorlds.size());
            for (int i = 0; i < batchSize && dispatched < chunksPerTick && !queue.isEmpty(); i++) {
                ChunkCoord coord = queue.poll();
                // Double-check: skip if already generated since queueing
                if (!Util.isChunkGenerated(coord.world(), coord.chunkX(), coord.chunkZ())) {
                    Util.getChunkAtAsync(coord.world(), coord.chunkX(), coord.chunkZ());
                    dispatched++;
                }
            }

            roundRobinIndex++;
            worldsChecked++;
        }
    }

    /**
     * Rebuilds the active worlds list from non-empty queues.
     */
    private void updateActiveWorlds() {
        activeWorlds.clear();
        chunkQueues.forEach((world, queue) -> {
            if (!queue.isEmpty()) {
                activeWorlds.add(world);
            }
        });
        roundRobinIndex = 0;
    }

    /**
     * Shuts down the pre-generation task.
     */
    public void shutdown() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        chunkQueues.clear();
        activeWorlds.clear();
    }
}
