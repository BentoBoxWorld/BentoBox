package world.bentobox.bentobox.nms;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;

/**
 * Pure-Bukkit implementation of {@link WorldRegenerator}.
 *
 * <p>Delegates to {@link org.bukkit.World#regenerateChunk(int, int)}, which
 * is deprecated by Bukkit but still functional in Paper. Used by
 * {@code CleanSuperFlatListener} to repaint stray superflat chunks loaded
 * inside an island world when the world was created without the
 * gamemode's chunk generator.
 */
public class WorldRegeneratorImpl implements WorldRegenerator {

    @Override
    @SuppressWarnings({ "deprecation", "removal" })
    public CompletableFuture<Void> regenerateChunk(Chunk chunk) {
        chunk.getWorld().regenerateChunk(chunk.getX(), chunk.getZ());
        return CompletableFuture.completedFuture(null);
    }
}
