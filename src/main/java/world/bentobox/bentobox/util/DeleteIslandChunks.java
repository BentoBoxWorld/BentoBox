package world.bentobox.bentobox.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.nms.WorldRegenerator;

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
    private final WorldRegenerator regenerator;

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
        // Regenerator
        this.regenerator = Util.getRegenerator();
        if (regenerator == null) {
            plugin.logError("Could not delete chunks because of NMS error");
            return;
        }

        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETE_CHUNKS).build();
        regenerateChunks();

    }

    private void regenerateChunks() {
        CompletableFuture<Void> all = plugin.getIWM().getAddon(di.getWorld())
                .map(gm -> new CompletableFuture[]{
                        processWorld(gm, di.getWorld()), // Overworld
                        processWorld(gm, netherWorld), // Nether
                        processWorld(gm, endWorld) // End
                })
                .map(CompletableFuture::allOf)
                .orElseGet(() -> CompletableFuture.completedFuture(null));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (all.isDone()) {
                    finish();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void finish() {
        // Fire event
        IslandEvent.builder().deletedIslandInfo(di).reason(Reason.DELETED).build();
        // We're done
        completed.set(true);
    }

    private CompletableFuture<Void> processWorld(GameModeAddon gm, World world) {
        if (world != null) {
            return regenerator.regenerate(gm, di, world);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }
}
