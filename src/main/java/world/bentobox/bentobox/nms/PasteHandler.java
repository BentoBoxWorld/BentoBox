package world.bentobox.bentobox.nms;

import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A helper class for {@link world.bentobox.bentobox.blueprints.BlueprintPaster}
 */
public interface PasteHandler {
    /**
     * Create a future to paste the blocks
     *
     * @param island   the island
     * @param world    the world
     * @param blockMap the block map
     * @return the future
     */
    CompletableFuture<Void> pasteBlocks(Island island, World world, Map<Location, BlueprintBlock> blockMap);

    /**
     * Create a future to paste the entities
     *
     * @param island    the island
     * @param world     the world
     * @param entityMap the entities map
     * @return the future
     */
    CompletableFuture<Void> pasteEntities(Island island, World world, Map<Location, List<BlueprintEntity>> entityMap);
}
