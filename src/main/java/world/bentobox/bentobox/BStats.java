package world.bentobox.bentobox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import world.bentobox.bentobox.api.addons.GameModeAddon;

/**
 * @author Poslovitch
 */
public class BStats {

    private static final int BSTATS_ID = 3555;

    private final BentoBox plugin;
    private Metrics metrics;

    /**
     * Counts of the islands that got created.
     * It is reset to 0 when BStats gets the data.
     * @since 1.1
     */
    private int islandsCreatedCount = 0;

    /**
     * Contains the amount of connected players since last data send.
     * @since 1.17.1
     */
    private final Set<UUID> connectedPlayerSet = new HashSet<>();


    BStats(BentoBox plugin) {
        this.plugin = plugin;
    }

    void registerMetrics() {
        if (metrics == null) {
            metrics = new Metrics(plugin, BSTATS_ID);
            registerCustomMetrics();
        }
    }

    private void registerCustomMetrics() {
        // Pie Charts
        registerDefaultLanguageChart();
        registerDatabaseTypeChart();
        registerAddonsChart();
        registerGameModeAddonsChart();
        registerHooksChart();
        registerPlayersPerServerChart();

        // Single Line charts
        registerIslandsCountChart();
        registerIslandsCreatedChart();

        // Bar Charts
        registerAddonsBarChart();
        registerGameModeAddonsBarChart();
        registerHooksBarChart();
    }

    private void registerDefaultLanguageChart() {
        metrics.addCustomChart(new SimplePie("default_language", () -> plugin.getSettings().getDefaultLanguage()));
    }

    private void registerDatabaseTypeChart() {
        metrics.addCustomChart(new SimplePie("database_type", () -> plugin.getSettings().getDatabaseType().toString()));
    }

    private void registerIslandsCountChart() {
        metrics.addCustomChart(new SingleLineChart("islands", () -> plugin.getIslands().getIslandCount()));
    }

    /**
     * @since 1.1
     */
    private void registerIslandsCreatedChart() {
        metrics.addCustomChart(new SingleLineChart("islandsCreated", () -> {
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

    /**
     * Adds given UUID to the connected player set.
     * @param uuid UUID of a player who logins.
     * @since 1.17.1
     */
    public void addPlayer(UUID uuid) {
        this.connectedPlayerSet.add(uuid);
    }

    /**
     * Sends the enabled addons (except GameModeAddons) of this server.
     * @since 1.1
     */
    private void registerAddonsChart() {
        metrics.addCustomChart(new AdvancedPie("addons", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getAddonsManager().getEnabledAddons().stream()
                    .filter(addon -> !(addon instanceof GameModeAddon) && addon.getDescription().isMetrics())
                    .forEach(addon -> values.put(addon.getDescription().getName(), 1));
            return values;
        }));
    }

    /**
     * Sends the enabled GameModeAddons of this server.
     * @since 1.4.0
     */
    private void registerGameModeAddonsChart() {
        metrics.addCustomChart(new AdvancedPie("gameModeAddons", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getAddonsManager().getGameModeAddons().stream()
                    .filter(gameModeAddon -> gameModeAddon.getDescription().isMetrics())
                    .forEach(gameModeAddon -> values.put(gameModeAddon.getDescription().getName(), 1));
            return values;
        }));
    }

    /**
     * Sends the enabled Hooks of this server.
     * @since 1.6.0
     */
    private void registerHooksChart() {
        metrics.addCustomChart(new AdvancedPie("hooks", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getHooks().getHooks().forEach(hook -> values.put(hook.getPluginName(), 1));
            return values;
        }));
    }

    /**
     * Sends the "category" this server is in depending on how many players it has.
     * @since 1.6.0
     */
    private void registerPlayersPerServerChart() {
        metrics.addCustomChart(new SimplePie("playersPerServer", () -> {
            int players = this.connectedPlayerSet.size();
            this.connectedPlayerSet.clear();

            if (players <= 0) return "0";
            else if (players <= 10) return "1-10";
            else if (players <= 30) return "11-30";
            else if (players <= 50) return "31-50";
            else if (players <= 100) return "51-100";
            else if (players <= 150) return "101-150";
            else if (players <= 200) return "151-200";
            else return "201+";
        }));
    }

    /**
     * Sends the enabled addons (except GameModeAddons) of this server as bar chart.
     * @since 1.17.1
     */
    private void registerAddonsBarChart() {
        metrics.addCustomChart(new SimpleBarChart("addonsBar", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getAddonsManager().getEnabledAddons().stream()
                .filter(addon -> !(addon instanceof GameModeAddon) && addon.getDescription().isMetrics())
                .forEach(addon -> values.put(addon.getDescription().getName(), 1));
            return values;
        }));
    }

    /**
     * Sends the enabled GameModeAddons of this server as a bar chart.
     * @since 1.17.1
     */
    private void registerGameModeAddonsBarChart() {
        metrics.addCustomChart(new SimpleBarChart("gameModeAddonsBar", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getAddonsManager().getGameModeAddons().stream()
                .filter(gameModeAddon -> gameModeAddon.getDescription().isMetrics())
                .forEach(gameModeAddon -> values.put(gameModeAddon.getDescription().getName(), 1));
            return values;
        }));
    }

    /**
     * Sends the enabled Hooks of this server as a bar chart.
     * @since 1.17.1
     */
    private void registerHooksBarChart() {
        metrics.addCustomChart(new SimpleBarChart("hooksBar", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getHooks().getHooks().forEach(hook -> values.put(hook.getPluginName(), 1));
            return values;
        }));
    }
}
