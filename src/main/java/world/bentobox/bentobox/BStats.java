package world.bentobox.bentobox;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.flags.Flag;

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
        registerFlagsDisplayModeChart();

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

    /**
     * Sends the enabled addons (except GameModeAddons) of this server.
     * @since 1.1
     */
    private void registerAddonsChart() {
        metrics.addCustomChart(new Metrics.AdvancedPie("addons", () -> {
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
        metrics.addCustomChart(new Metrics.AdvancedPie("gameModeAddons", () -> {
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
        metrics.addCustomChart(new Metrics.AdvancedPie("hooks", () -> {
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
        metrics.addCustomChart(new Metrics.SimplePie("playersPerServer", () -> {
            int players = Bukkit.getOnlinePlayers().size();
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
     * Sends the "flags display mode" of all the online players.
     * @since 1.6.0
     */
    private void registerFlagsDisplayModeChart() {
        metrics.addCustomChart(new Metrics.AdvancedPie("flagsDisplayMode", () -> {
            Map<String, Integer> values = new HashMap<>();

            Bukkit.getOnlinePlayers().forEach(player -> {
                Flag.Mode mode = plugin.getPlayers().getFlagsDisplayMode(player.getUniqueId());
                if (values.containsKey(mode.name())) {
                    values.put(mode.name(), values.get(mode.name()) + 1);
                } else {
                    values.put(mode.name(), 1);
                }
            });

            return values;
        }));
    }
}
