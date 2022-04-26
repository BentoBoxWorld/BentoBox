package world.bentobox.bentobox.nms;

import org.bukkit.Location;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A helper class for {@link world.bentobox.bentobox.blueprints.BlueprintPaster}
 */
public interface NMSPaster {
    /**
     * Create a future to paste the blocks
     *
     * @param island   the island
     * @param blockMap the block map
     * @return the future
     */
    CompletableFuture<Void> pasteBlocks(Island island, Map<Location, BlueprintBlock> blockMap);

    /**
     * Create a future to paste the entities
     *
     * @param island    the island
     * @param entityMap the entities map
     * @return the future
     */
    CompletableFuture<Void> pasteEntities(Island island, Map<Location, List<BlueprintEntity>> entityMap);
}
