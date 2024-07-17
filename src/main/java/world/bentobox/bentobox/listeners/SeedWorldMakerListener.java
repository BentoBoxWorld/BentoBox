package world.bentobox.bentobox.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.util.Util;

/**
 * Updates chunks in seed worlds if they have been generated in the main world
 * @author tastybento
 */
public class SeedWorldMakerListener implements Listener {

    private final BentoBox plugin;

    /**
     * Whether BentoBox is ready or not.
     * This helps to avoid hanging out the server on startup as a lot of {@link ChunkLoadEvent} are called at this time.
     * @since 1.1
     */
    private boolean ready;


    public SeedWorldMakerListener(BentoBox bentoBox) {
        this.plugin = bentoBox;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        ready = true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!ready) {
            return;
        }
        World world = e.getWorld();
        plugin.getIWM().getAddon(world).filter(GameModeAddon::isUsesNewChunkGeneration).ifPresent(gma -> {
            World seed = Bukkit.getWorld(world.getName() + "/bentobox");
            int x = e.getChunk().getX();
            int z = e.getChunk().getZ();
            if (seed != null && !seed.getChunkAt(x, z, false).isGenerated()) {
                Util.getChunkAtAsync(seed, x, z, true);
            }
        });

    }



}
