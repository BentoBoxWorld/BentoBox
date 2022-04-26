package world.bentobox.bentobox.nms;

import org.bukkit.World;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.IslandDeletion;

import java.util.concurrent.CompletableFuture;

public interface NMSAbstraction {
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
