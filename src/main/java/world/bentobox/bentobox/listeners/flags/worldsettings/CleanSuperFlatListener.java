package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.nms.WorldRegenerator;
import world.bentobox.bentobox.util.Util;

/**
 * Cleans super-flat world chunks or normal nether chunks if they generate accidentally
 * due to lack of a generator being loaded
 * @author tastybento
 */
public class CleanSuperFlatListener extends FlagListener {

    private final BentoBox plugin = BentoBox.getInstance();

    /**
     * Stores chunks that need to be regenerated.
     */
    @NonNull
    private final Queue<Chunk> chunkQueue = new LinkedList<>();

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

    private WorldRegenerator regenerator;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        this.regenerator = Util.getRegenerator();
        if (regenerator == null) {
            plugin.logError("Could not start CleanSuperFlat because of NMS error");
            return;
        }
        ready = true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) 
    {
        World world = e.getWorld();
        
        if (this.noClean(world, e)) 
        {
            return;
        }
        
        ChunkGenerator cg = plugin.getAddonsManager().getDefaultWorldGenerator(world.getName(), "");
        
        if (cg == null) 
        {
            Flags.CLEAN_SUPER_FLAT.setSetting(world, false);

            this.plugin.logWarning("Could not enable Clean Super Flat for " + world.getName());
            this.plugin.logWarning("There is no world generator assigned to this world.");
            this.plugin.logWarning("This is often caused by the 'use-own-generator' being set to 'true' in the gamemode's" +
                    " configuration while there hasn't been any custom world generator assigned to the world.");
            this.plugin.logWarning("Either revert the changes in the gamemode's config.yml or assign your custom world generator to the world.");

            return;
        }
        
        // Add to queue
        this.chunkQueue.add(e.getChunk());
        
        if (this.task == null || this.task.isCancelled())
        {
            this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.cleanChunk(world, cg), 0L, 1L);
        }
    }


    /**
     * This method clears the chunk from queue in the given world
     * @param world The world that must be cleared.
     * @param cg Chunk generator.
     */
    private void cleanChunk(World world, ChunkGenerator cg)
    {
        if (!this.chunkQueue.isEmpty())
        {
            Chunk chunk = this.chunkQueue.poll();

            regenerator.regenerateChunk(chunk);
            
            if (this.plugin.getSettings().isLogCleanSuperFlatChunks())
            {
                this.plugin.log("Regenerating superflat chunk in " + world.getName() +
                    " at (" + chunk.getX() + ", " + chunk.getZ() + ") " +
                    "(" + this.chunkQueue.size() + " chunk(s) remaining in the queue)");
            }
        }
        else
        {
            this.task.cancel();
        }
    }
    

    /**
     * Check if chunk should be cleaned or not
     * @param world - world
     * @param e chunk load event
     * @return true if the chunk should not be cleaned
     */
    private boolean noClean(World world, ChunkLoadEvent e) {
        if (!this.ready)
        {
            return true;
        }
        // Check if super-flat must even be working.
        if (!this.getIWM().inWorld(world) ||
            !Flags.CLEAN_SUPER_FLAT.isSetForWorld(world) ||
            world.getEnvironment().equals(Environment.NETHER) &&
                (!this.plugin.getIWM().isNetherGenerate(world) || !this.plugin.getIWM().isNetherIslands(world)) ||
            world.getEnvironment().equals(Environment.THE_END) &&
                (!this.plugin.getIWM().isEndGenerate(world) || !this.plugin.getIWM().isEndIslands(world)))
        {
            return true;
        }

        // Check if bottom is a super-flat chunk.
        int minHeight = world.getMinHeight();

        // Due to flat super flat chunk generation changes in 1.19, they now are generated properly at the world min.
        // Extra check for 0-4 can be removed with 1.18 dropping.

        return !(e.getChunk().getBlock(0, 0, 0).getType().equals(Material.BEDROCK) &&
            e.getChunk().getBlock(0, 1, 0).getType().equals(Material.DIRT) &&
            e.getChunk().getBlock(0, 2, 0).getType().equals(Material.DIRT) &&
            e.getChunk().getBlock(0, 3, 0).getType().equals(Material.GRASS_BLOCK)) &&
            !(e.getChunk().getBlock(0, minHeight, 0).getType().equals(Material.BEDROCK) &&
                e.getChunk().getBlock(0, minHeight + 1, 0).getType().equals(Material.DIRT) &&
                e.getChunk().getBlock(0, minHeight + 2, 0).getType().equals(Material.DIRT) &&
                e.getChunk().getBlock(0, minHeight + 3, 0).getType().equals(Material.GRASS_BLOCK));
    }
}
