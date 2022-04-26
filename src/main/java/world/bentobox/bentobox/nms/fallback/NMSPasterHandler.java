package world.bentobox.bentobox.nms.fallback;

import org.bukkit.Location;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.NMSPaster;
import world.bentobox.bentobox.util.DefaultPasterUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NMSPasterHandler implements NMSPaster {
    @Override
    public CompletableFuture<Void> pasteBlocks(Island island, Map<Location, BlueprintBlock> blockMap) {
        blockMap.forEach((location, block) -> DefaultPasterUtil.setBlock(island, location, block));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pasteEntities(Island island, Map<Location, List<BlueprintEntity>> entityMap) {
        entityMap.forEach((location, blueprintEntities) -> DefaultPasterUtil.setEntity(island, location, blueprintEntities));
        return CompletableFuture.completedFuture(null);
    }
}
