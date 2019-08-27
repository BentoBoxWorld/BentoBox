package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.MyBiomeGrid;
import world.bentobox.bentobox.util.Pair;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 */
public class CleanSuperFlatListener extends FlagListener {

    private BentoBox plugin = BentoBox.getInstance();

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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        World world = e.getWorld();
        if (noClean(world, e)) {
            return;
        }
        MyBiomeGrid grid = new MyBiomeGrid(world.getEnvironment());
        ChunkGenerator cg = plugin.getAddonsManager().getDefaultWorldGenerator(world.getName(), "");
        if (cg == null) {
            Flags.CLEAN_SUPER_FLAT.setSetting(world, false);

            plugin.logWarning("Could not enable Clean Super Flat for " + world.getName());
            plugin.logWarning("There is no world generator assigned to this world.");
            plugin.logWarning("This is often caused by the 'use-own-generator' being set to 'true' in the gamemode's" +
                    " configuration while there hasn't been any custom world generator assigned to the world.");
            plugin.logWarning("Either revert the changes in the gamemode's config.yml or assign your custom world generator to the world.");

            return;
        }
        // Add to queue
        chunkQueue.add(new Pair<>(e.getChunk().getX(), e.getChunk().getZ()));
        if (task == null || task.isCancelled()) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> cleanChunk(e, world, cg, grid), 0L, 1L);
        }
    }

    private void cleanChunk(ChunkLoadEvent e, World world, ChunkGenerator cg, MyBiomeGrid grid) {
        SecureRandom random = new SecureRandom();
        if (!chunkQueue.isEmpty()) {
            Pair<Integer, Integer> chunkXZ = chunkQueue.poll();
            ChunkData cd = cg.generateChunkData(world, random, e.getChunk().getX(), e.getChunk().getZ(), grid);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < world.getMaxHeight(); y++) {
                        e.getChunk().getBlock(x, y, z).setBlockData(cd.getBlockData(x, y, z), false);
                    }
                }
            }
            // Run populators
            cg.getDefaultPopulators(world).forEach(pop -> pop.populate(world, random, e.getChunk()));
            if (plugin.getSettings().isLogCleanSuperFlatChunks()) {
                plugin.log("Regenerating superflat chunk in " + world.getName() + " at (" + chunkXZ.x + ", " + chunkXZ.z + ") " +
                        "(" + chunkQueue.size() + " chunk(s) remaining in the queue)");
            }
        } else {
            task.cancel();
        }
    }

    /**
     * Check if chunk should be cleaned or not
     * @param world - world
     * @param e chunk load event
     * @return true if the chunk should not be cleaned
     */
    private boolean noClean(World world, ChunkLoadEvent e) {
        if (!ready) {
            return true;
        }
        return !getIWM().inWorld(world) || !Flags.CLEAN_SUPER_FLAT.isSetForWorld(world) ||
                (!(e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK)
                        && e.getChunk().getBlock(0, 1, 0).getType().equals(Material.DIRT)
                        && e.getChunk().getBlock(0, 2, 0).getType().equals(Material.DIRT)
                        && e.getChunk().getBlock(0, 3, 0).getType().equals(Material.GRASS_BLOCK))
                        || (world.getEnvironment().equals(Environment.NETHER) && (!plugin.getIWM().isNetherGenerate(world)
                                || !plugin.getIWM().isNetherIslands(world)))
                        || (world.getEnvironment().equals(Environment.THE_END) && (!plugin.getIWM().isEndGenerate(world)
                                || !plugin.getIWM().isEndIslands(world))));
    }
}
