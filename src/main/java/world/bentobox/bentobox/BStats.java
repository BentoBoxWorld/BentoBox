package world.bentobox.bentobox;

import org.bstats.bukkit.Metrics;

/**
 * @author Poslovitch
 */
public class BStats {

    private final BentoBox plugin;
    private Metrics metrics;

    /**
     * Counts of the islands that got created.
     * It is reset to 0 when BStats gets the data.
     * @since 1.1
     */
    private int islandsCreatedCount = 0;

    BStats(BentoBox plugin) {
        this.plugin = plugin;
    }

    void registerMetrics() {
        if (metrics == null) {
            metrics = new Metrics(plugin);
            registerCustomMetrics();
        }
    }

    private void registerCustomMetrics() {
        // Simple Pie Charts
        registerDefaultLanguageChart();
        registerDatabaseTypeChart();

        // Single Line charts
        registerIslandsCountChart();
        registerIslandsCreatedChart();
    }

    private void registerDefaultLanguageChart() {
        metrics.addCustomChart(new Metrics.SimplePie("default_language", () -> plugin.getSettings().getDefaultLanguage()));
    }

    private void registerDatabaseTypeChart() {
        metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> plugin.getSettings().getDatabaseType().toString()));
    }

    private void registerIslandsCountChart() {
        metrics.addCustomChart(new Metrics.SingleLineChart("islands", () -> plugin.getIslands().getIslandCount()));
    }

    /**
     * @since 1.1
     */
    private void registerIslandsCreatedChart() {
        metrics.addCustomChart(new Metrics.SingleLineChart("islandsCreated", () -> {
            int value = islandsCreatedCount;
            islandsCreatedCount = 0;
            return value;
        }));
    }

    /**
     * Increases the count of islands that got create since the last "data get" request from BStats.
     * @since 1.1
     */
    public void increaseIslandsCreatedCount() {
        islandsCreatedCount++;
    }
}
