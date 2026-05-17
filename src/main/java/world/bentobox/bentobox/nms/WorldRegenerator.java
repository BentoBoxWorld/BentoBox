package world.bentobox.bentobox.nms;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;

/**
 * Hook for regenerating chunks. Used by {@code CleanSuperFlatListener}
 * to repaint stray superflat chunks loaded inside an island world.
 */
public interface WorldRegenerator {
    /**
     * Regenerate a specific chunk to what it should be. Mainly used by clear super flat.
     * @param chunk chunk to be regenerated
     * @return future when it is done
     */
    CompletableFuture<Void> regenerateChunk(Chunk chunk);
}
