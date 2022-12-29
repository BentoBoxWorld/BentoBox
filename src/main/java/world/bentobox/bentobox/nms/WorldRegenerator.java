package world.bentobox.bentobox.nms;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.IslandDeletion;

/**
 * A world generator used by {@link world.bentobox.bentobox.util.DeleteIslandChunks}
 */
public interface WorldRegenerator {
    /**
     * Create a future to regenerate the regions of the island.
     *
     * @param gm     the game mode
     * @param di     the island deletion
     * @param world  the world
     * @return the completable future
     */
    CompletableFuture<Void> regenerate(GameModeAddon gm, IslandDeletion di, World world);
}
