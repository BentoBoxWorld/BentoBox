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
            registerCustomMetrics();
        }
    }

    private void registerCustomMetrics() {
        // Simple Pie Charts
        registerDefaultLanguageChart();
        registerDatabaseTypeChart();

        // Single Line charts
        registerIslandsCountChart();
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
}
