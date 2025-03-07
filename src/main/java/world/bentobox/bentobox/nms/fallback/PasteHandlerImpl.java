package world.bentobox.bentobox.nms.fallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.DefaultPasteUtil;

public class PasteHandlerImpl implements PasteHandler {
    @Override
    public CompletableFuture<Void> pasteBlocks(Island island, World world, Map<Location, BlueprintBlock> blockMap) {
        return blockMap.entrySet().stream()
                .map(entry -> setBlock(island, entry.getKey(), entry.getValue()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))
                        )
                );
    }

    @Override
    public CompletableFuture<Void> setBlock(Island island, Location location, BlueprintBlock bpBlock) {
        return DefaultPasteUtil.setBlock(island, location, bpBlock);
    }

    @Override
    public Block setBlock(Location location, BlockData blockData) {
        Block block = location.getBlock();
        block.setBlockData(blockData);
        return block;
    }

}
