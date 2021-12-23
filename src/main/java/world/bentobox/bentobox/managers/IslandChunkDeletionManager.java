package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.DeleteIslandChunks;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class IslandChunkDeletionManager implements Runnable {
    private final boolean slowDeletion;
    private final BentoBox plugin;
    private final AtomicReference<DeleteIslandChunks> currentTask;
    private final Queue<IslandDeletion> queue;

    public IslandChunkDeletionManager(BentoBox plugin) {
        this.plugin = plugin;
        this.currentTask = new AtomicReference<>();
        this.queue = new LinkedList<>();
        this.slowDeletion = plugin.getSettings().isSlowDeletion();

        if (slowDeletion) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0L, 20L);
        }
    }

    @Override
    public void run() {
        if (queue.isEmpty()) {
            return;
        }
        DeleteIslandChunks task = this.currentTask.get();
        if (task != null && !task.isCompleted()) {
            return;
        }
        IslandDeletion islandDeletion = queue.remove();
        currentTask.set(new DeleteIslandChunks(plugin, islandDeletion));
    }

    public void add(IslandDeletion islandDeletion) {
        if (slowDeletion) {
            queue.add(islandDeletion);
        } else {
            new DeleteIslandChunks(plugin, islandDeletion);
        }
    }
}
