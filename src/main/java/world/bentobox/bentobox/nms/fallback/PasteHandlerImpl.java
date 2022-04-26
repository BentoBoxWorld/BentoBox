package world.bentobox.bentobox.nms.fallback;

import org.bukkit.Location;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.DefaultPasteUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PasteHandlerImpl implements PasteHandler {
    @Override
    public CompletableFuture<Void> pasteBlocks(Island island, Map<Location, BlueprintBlock> blockMap) {
        blockMap.forEach((location, block) -> DefaultPasteUtil.setBlock(island, location, block));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pasteEntities(Island island, Map<Location, List<BlueprintEntity>> entityMap) {
        entityMap.forEach((location, blueprintEntities) -> DefaultPasteUtil.setEntity(island, location, blueprintEntities));
        return CompletableFuture.completedFuture(null);
    }
}
