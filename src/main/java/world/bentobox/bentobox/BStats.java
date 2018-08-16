package world.bentobox.bentobox;

import org.bstats.bukkit.Metrics;

/**
 * @author Poslovitch
 */
class BStats {

    private final BentoBox plugin;
    private Metrics metrics;

    BStats(BentoBox plugin) {
        this.plugin = plugin;
    }

    void registerMetrics() {
        if (metrics == null) {
            metrics = new Metrics(plugin);
        }
    }
}
