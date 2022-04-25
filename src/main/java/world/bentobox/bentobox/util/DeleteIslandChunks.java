package world.bentobox.bentobox.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.nms.NMSAbstraction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deletes islands chunk by chunk
 *
 * @author tastybento
 */
public class DeleteIslandChunks {

    private final IslandDeletion di;
    private final BentoBox plugin;
    private final World netherWorld;
    private final World endWorld;
    private final AtomicBoolean completed;
    private final NMSAbstraction nms;
    private int chunkX;
    private int chunkZ;
    private BukkitTask task;
    private CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        this.plugin = plugin;
        this.chunkX = di.getMinXChunk();
        this.chunkZ = di.getMinZChunk();
        this.di = di;
        completed = new AtomicBoolean(false);
        // Nether
        if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
            netherWorld = plugin.getIWM().getNetherWorld(di.getWorld());
        } else {
            netherWorld = null;
        }
        // End
        if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
            endWorld = plugin.getIWM().getEndWorld(di.getWorld());
        } else {
            endWorld = null;
        }
        // NMS
        this.nms = Util.getNMS();
        if (nms == null) {
            plugin.logError("Could not delete chunks because of NMS error");
            return;
        }

        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        regenerateChunks();

    }

    private void regenerateChunks() {
        // Run through all chunks of the islands and regenerate them.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!currentTask.isDone()) return;
            if (isEnded(chunkX)) {
                finish();
                return;
            }
            List<CompletableFuture<Void>> newTasks = new ArrayList<>();
            for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                if (isEnded(chunkX)) {
                    break;
                }
                final int x = chunkX;
                final int z = chunkZ;
                plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm -> {
                    newTasks.add(processChunk(gm, di.getWorld(), x, z)); // Overworld
                    newTasks.add(processChunk(gm, netherWorld, x, z)); // Nether
                    newTasks.add(processChunk(gm, endWorld, x, z)); // End
                });
                chunkZ++;
                if (chunkZ > di.getMaxZChunk()) {
                    chunkZ = di.getMinZChunk();
                    chunkX++;
                }
            }
            currentTask = CompletableFuture.allOf(newTasks.toArray(new CompletableFuture[0]));
        }, 0L, 20L);
    }

    private boolean isEnded(int chunkX) {
        return chunkX > di.getMaxXChunk();
    }

    private void finish() {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
        // We're done
        completed.set(true);
        task.cancel();
    }

    private CompletableFuture<Void> processChunk(GameModeAddon gm, World world, int x, int z) {
        if (world != null) {
            return nms.regenerateChunk(gm, di, world, x, z);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }
}
