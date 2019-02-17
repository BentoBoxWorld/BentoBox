package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Pair;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 */
public class CleanSuperFlatListener extends FlagListener {

    /**
     * Stores pairs of X,Z coordinates of chunks that need to be regenerated.
     * @since 1.1
     */
    @NonNull
    private Queue<@NonNull Pair<@NonNull Integer, @NonNull Integer>> chunkQueue = new LinkedList<>();

    /**
     * Task that runs each tick to regenerate chunks that are in the {@link #chunkQueue}.
     * It regenerates them one at a time.
     * @since 1.1
     */
    @Nullable
    private BukkitTask task;

    /**
     * Whether BentoBox is ready or not.
     * This helps to avoid hanging out the server on startup as a lot of {@link ChunkLoadEvent} are called at this time.
     * @since 1.1
     */
    private boolean ready;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        ready = true;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!ready) {
            return;
        }
        BentoBox plugin = BentoBox.getInstance();
        World world = e.getWorld();
        if (!e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK)
                || !Flags.CLEAN_SUPER_FLAT.isSetForWorld(world)
                || (world.getEnvironment().equals(Environment.NETHER) && (!plugin.getIWM().isNetherGenerate(world) || !plugin.getIWM().isNetherIslands(world)))
                || (world.getEnvironment().equals(Environment.THE_END) && (!plugin.getIWM().isEndGenerate(world) || !plugin.getIWM().isEndIslands(world)))) {
            return;
        }
        // Add to queue
        chunkQueue.add(new Pair<>(e.getChunk().getX(), e.getChunk().getZ()));
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!chunkQueue.isEmpty()) {
                    Pair<Integer, Integer> chunkXZ = chunkQueue.poll();
                    world.regenerateChunk(chunkXZ.x, chunkXZ.z); // NOSONAR - the deprecation doesn't cause any issues to us
                    if (plugin.getSettings().isLogCleanSuperFlatChunks()) {
                        plugin.log(chunkQueue.size() + " Regenerating superflat chunk " + world.getName() + " " + chunkXZ.x + ", " + chunkXZ.z);
                    }
                }
            }, 0L, 1L);
        }
    }
}
