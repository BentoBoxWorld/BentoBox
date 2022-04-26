package world.bentobox.bentobox.nms;

import org.bukkit.Location;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NMSPaster {
    CompletableFuture<Void> pasteBlocks(Island island, Map<Location, BlueprintBlock> blockMap);

    CompletableFuture<Void> pasteEntities(Island island, Map<Location, List<BlueprintEntity>> entityMap);
}
