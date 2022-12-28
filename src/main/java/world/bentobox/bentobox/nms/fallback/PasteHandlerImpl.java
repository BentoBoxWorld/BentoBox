package world.bentobox.bentobox.nms.fallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.DefaultPasteUtil;

public class PasteHandlerImpl implements PasteHandler {
    @Override
    public CompletableFuture<Void> pasteBlocks(Island island, World world, Map<Location, BlueprintBlock> blockMap) {
        return blockMap.entrySet().stream()
                .map(entry -> DefaultPasteUtil.setBlock(island, entry.getKey(), entry.getValue()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))
                        )
                );
    }

    @Override
    public CompletableFuture<Void> pasteEntities(Island island, World world, Map<Location, List<BlueprintEntity>> entityMap) {
        return entityMap.entrySet().stream()
                .map(entry -> DefaultPasteUtil.setEntity(island, entry.getKey(), entry.getValue()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))
                        )
                );
    }
}
