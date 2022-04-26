package world.bentobox.bentobox.util;

import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.nms.NMSAbstraction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deletes islands chunk by chunk
 *
 * @author tastybento
 */
public class DeleteIslandChunks {

    private final IslandDeletion di;
    private final BentoBox plugin;
    private final World netherWorld;
    private final World endWorld;
    private final AtomicBoolean completed;
    private final NMSAbstraction nms;

    public DeleteIslandChunks(BentoBox plugin, IslandDeletion di) {
        this.plugin = plugin;
        this.di = di;
        completed = new AtomicBoolean(false);
        // Nether
        if (plugin.getIWM().isNetherGenerate(di.getWorld()) && plugin.getIWM().isNetherIslands(di.getWorld())) {
            netherWorld = plugin.getIWM().getNetherWorld(di.getWorld());
        } else {
            netherWorld = null;
        }
        // End
        if (plugin.getIWM().isEndGenerate(di.getWorld()) && plugin.getIWM().isEndIslands(di.getWorld())) {
            endWorld = plugin.getIWM().getEndWorld(di.getWorld());
        } else {
            endWorld = null;
        }
        // NMS
        this.nms = Util.getNMS();
        if (nms == null) {
            plugin.logError("Could not delete chunks because of NMS error");
            return;
        }

        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        regenerateChunks();

    }

    private void regenerateChunks() {
        List<CompletableFuture<Void>> futures = new ArrayList<>(3);
        plugin.getIWM().getAddon(di.getWorld()).ifPresent(gm -> {
            futures.add(processWorld(gm, di.getWorld())); // Overworld
            futures.add(processWorld(gm, netherWorld)); // Nether
            futures.add(processWorld(gm, endWorld)); // End
        });
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        plugin.logStacktrace(t);
                    }
                    finish();
                });
    }

    private void finish() {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
        // We're done
        completed.set(true);
    }

    private CompletableFuture<Void> processWorld(GameModeAddon gm, World world) {
        if (world != null) {
            return nms.regenerate(gm, di, world);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }
}
