package world.bentobox.bentobox.nms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.DefaultPasteUtil;

/**
 * A helper class for {@link world.bentobox.bentobox.blueprints.BlueprintPaster}
 */
public interface PasteHandler {

    BlockData AIR_BLOCKDATA = Bukkit.createBlockData(Material.AIR);
    /**
     * Create a future to paste the blocks
     *
     * @param island   the island
     * @param world    the world
     * @param blockMap the block map
     * @return the future
     */
    default CompletableFuture<Void> pasteBlocks(Island island, World world, Map<Location, BlueprintBlock> blockMap) {
        return blockMap.entrySet().stream().map(entry -> setBlock(island, entry.getKey(), entry.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))));
    }

    CompletableFuture<Void> setBlock(Island island, Location location, BlueprintBlock bpBlock);

    /**
     * Set the block at location to the block data
     * @param location location
     * @param blockData block data
     * @return block
     */
    Block setBlock(Location location, BlockData blockData);

    /**
     * Create a future to paste the entities
     *
     * @param island    the island
     * @param world     the world
     * @param entityMap the entities map
     * @return the future
     */
    default CompletableFuture<Void> pasteEntities(Island island, World world,
            Map<Location, List<BlueprintEntity>> entityMap) {
        return entityMap.entrySet().stream()
                .map(entry -> DefaultPasteUtil.setEntity(island, entry.getKey(), entry.getValue()))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))));
    }
}
