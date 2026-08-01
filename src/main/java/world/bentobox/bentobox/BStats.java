package world.bentobox.bentobox;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;

/**
 * @author Poslovitch
 */
public class BStats {

    private static final int BSTATS_ID = 3555;

    /**
     * Maximum number of distinct command keys tracked between submissions. Once this
     * is reached, counts of already-known commands keep rising but new ones are
     * dropped. Guards against unbounded growth from addons with generated commands.
     * @since 3.22.0
     */
    private static final int MAX_TRACKED_COMMANDS = 200;

    /**
     * Maximum number of failing commands reported per submission. Only the busiest
     * are sent, to keep the chart readable and the payload small.
     * @since 3.22.0
     */
    private static final int REPORTED_COMMANDS = 20;

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

    /**
     * Counts of command failures by {@link CommandFailure} since the last data send.
     * @since 3.22.0
     */
    private final Map<String, Integer> commandFailures = new HashMap<>();

    /**
     * Counts of command failures by command key since the last data send.
     * @since 3.22.0
     */
    private final Map<String, Integer> commandFailurePaths = new HashMap<>();


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
        registerPlayersPerServerChart();
        registerAddonApiVersionsChart();
        registerCommandFailuresChart();

        // Drilldown Pie Charts
        registerAddonVersionsChart();
        registerGameModeVersionsChart();

        // Single Line charts
        registerIslandsCountChart();
        registerIslandsCreatedChart();

        // Bar Charts
        registerAddonsBarChart();
        registerGameModeAddonsBarChart();
        registerHooksBarChart();
        registerFeaturesChart();
        registerCommandFailurePathsChart();
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
     * Sends the "category" this server is in depending on how many players it has.
     * @since 1.6.0
     */
    private void registerPlayersPerServerChart() {
        metrics.addCustomChart(new SimplePie("playersPerServer", () -> {
            int players = this.connectedPlayerSet.size();
            this.connectedPlayerSet.clear();

            if (players == 0) return "0";
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

    /**
     * Sends the version of each enabled addon (except GameModeAddons), grouped by
     * addon name. This is what tells us how long the tail is after a release and
     * whether it is safe to make a breaking API change.
     * @since 3.22.0
     */
    private void registerAddonVersionsChart() {
        metrics.addCustomChart(new DrilldownPie("addonVersions",
                () -> versionsOf(plugin.getAddonsManager().getEnabledAddons().stream()
                        .filter(addon -> !(addon instanceof GameModeAddon)))));
    }

    /**
     * Sends the version of each enabled GameModeAddon, grouped by game mode name.
     * @since 3.22.0
     */
    private void registerGameModeVersionsChart() {
        metrics.addCustomChart(new DrilldownPie("gameModeVersions",
                () -> versionsOf(plugin.getAddonsManager().getGameModeAddons().stream())));
    }

    /**
     * Builds a name to version to count map for the given addons, honouring their
     * metrics opt-out.
     * @param addons stream of addons
     * @return map of addon name to a map of version to count
     * @since 3.22.0
     */
    private Map<String, Map<String, Integer>> versionsOf(Stream<? extends Addon> addons) {
        Map<String, Map<String, Integer>> values = new HashMap<>();
        addons.filter(addon -> addon.getDescription().isMetrics())
                .forEach(addon -> values.computeIfAbsent(addon.getDescription().getName(), k -> new HashMap<>())
                        .merge(addon.getDescription().getVersion(), 1, Integer::sum));
        return values;
    }

    /**
     * Sends the BentoBox API version each enabled addon is built against. Tells us
     * which API levels are still in use in the wild.
     * @since 3.22.0
     */
    private void registerAddonApiVersionsChart() {
        metrics.addCustomChart(new AdvancedPie("addonApiVersions", () -> {
            Map<String, Integer> values = new HashMap<>();
            plugin.getAddonsManager().getEnabledAddons().stream()
                    .filter(addon -> addon.getDescription().isMetrics())
                    .map(addon -> addon.getDescription().getApiVersion())
                    .filter(version -> version != null && !version.isEmpty())
                    .forEach(version -> values.merge(version, 1, Integer::sum));
            return values;
        }));
    }

    /**
     * Sends the settings this server has changed away from their default value.
     * <p>
     * Only deviations are reported, so every bar is a signal: a feature that
     * defaults to off appears under its own name when a server enables it, and a
     * feature that defaults to on appears as {@code no-<name>} when a server turns
     * it off. Comparing a bar against the total server count gives the adoption (or
     * rejection) rate of that feature.
     * @since 3.22.0
     */
    private void registerFeaturesChart() {
        metrics.addCustomChart(new SimpleBarChart("features", () -> {
            Map<String, Integer> values = new HashMap<>();
            Settings s = plugin.getSettings();
            // Features that default to off - reported when switched on
            optedIn(values, "economy-charges-for-blueprints", s.isChargeForBlueprintOnReset());
            optedIn(values, "name-uniqueness", s.isNameUniqueness());
            optedIn(values, "dialogs-gamemode-selection", s.isDialogGameModeSelection());
            optedIn(values, "head-cache-server", s.isUseCacheServer());
            optedIn(values, "slow-deletion", s.isSlowDeletion());
            optedIn(values, "keep-previous-island", s.isKeepPreviousIslandOnReset());
            optedIn(values, "housekeeping-age", s.isHousekeepingAgeEnabled());
            optedIn(values, "chunk-pregen", s.isPregenEnabled());
            optedIn(values, "teleport-remove-mobs", s.isTeleportRemoveMobs());
            optedIn(values, "hide-used-blueprints", s.isHideUsedBlueprints());
            optedIn(values, "override-safety-check", s.isOverrideSafetyCheck());
            // Features that default to on - reported when switched off
            optedOut(values, "economy", s.isUseEconomy());
            optedOut(values, "brigadier-commands", s.isUseBrigadierCommands());
            optedOut(values, "did-you-mean-unknown-commands", s.isDidYouMeanUnknownCommands());
            optedOut(values, "did-you-mean-subcommands", s.isDidYouMeanSubcommands());
            optedOut(values, "dialogs-confirmations", s.isDialogConfirmations());
            optedOut(values, "dialogs-team-invites", s.isDialogTeamInvites());
            optedOut(values, "dialogs-go-picker", s.isDialogGoPicker());
            optedOut(values, "close-panel-on-click-outside", s.isClosePanelOnClickOutside());
            optedOut(values, "dynmap-island-markers", s.isDynmapIslandMarkers());
            optedOut(values, "bluemap-island-markers", s.isBluemapIslandMarkers());
            optedOut(values, "housekeeping-deleted", s.isHousekeepingDeletedEnabled());
            optedOut(values, "update-check-bentobox", s.isCheckBentoBoxUpdates());
            optedOut(values, "update-check-addons", s.isCheckAddonsUpdates());
            return values;
        }));
    }

    /**
     * Records a feature that defaults to off and has been switched on.
     * @since 3.22.0
     */
    private void optedIn(Map<String, Integer> values, String name, boolean enabled) {
        if (enabled) {
            values.put(name, 1);
        }
    }

    /**
     * Records a feature that defaults to on and has been switched off.
     * @since 3.22.0
     */
    private void optedOut(Map<String, Integer> values, String name, boolean enabled) {
        if (!enabled) {
            values.put("no-" + name, 1);
        }
    }

    /**
     * Sends the number of command failures of each kind since the last data send.
     * @since 3.22.0
     */
    private void registerCommandFailuresChart() {
        metrics.addCustomChart(new AdvancedPie("commandFailures", () -> {
            Map<String, Integer> values = new HashMap<>(commandFailures);
            commandFailures.clear();
            return values;
        }));
    }

    /**
     * Sends the commands that failed most often since the last data send. Only the
     * busiest {@value #REPORTED_COMMANDS} are sent.
     * @since 3.22.0
     */
    private void registerCommandFailurePathsChart() {
        metrics.addCustomChart(new SimpleBarChart("commandFailurePaths", () -> {
            Map<String, Integer> values = commandFailurePaths.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(REPORTED_COMMANDS)
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            commandFailurePaths.clear();
            return values;
        }));
    }

    /**
     * Records that a command could not be run to completion.
     * <p>
     * Only the failure kind and the command's stable key are recorded - never what
     * the player typed - because bStats data is public.
     * @param failure kind of failure
     * @param commandKey stable identifier of the command, see
     *        {@link world.bentobox.bentobox.api.commands.CompositeCommand#getStatsKey()}
     * @since 3.22.0
     */
    public void recordCommandFailure(@NonNull CommandFailure failure, @NonNull String commandKey) {
        commandFailures.merge(failure.name().toLowerCase(Locale.ENGLISH), 1, Integer::sum);
        // Only track a new command if there is room, otherwise a badly behaved addon
        // could grow this map without limit between submissions.
        if (commandFailurePaths.containsKey(commandKey) || commandFailurePaths.size() < MAX_TRACKED_COMMANDS) {
            commandFailurePaths.merge(commandKey, 1, Integer::sum);
        }
    }

    /**
     * The ways in which running a command can fail.
     * @since 3.22.0
     */
    public enum CommandFailure {
        /**
         * The player typed something that matched none of the available subcommands
         * and a "did you mean" suggestion was offered instead.
         */
        UNKNOWN_SUBCOMMAND,
        /**
         * A player ran a console-only command, or the console ran a player-only one.
         */
        WRONG_CONTEXT,
        /**
         * The user lacked the permission for the command or one of its parents.
         */
        NO_PERMISSION,
        /**
         * The command refused to run in the current state: wrong world, no island,
         * insufficient rank, cooldown still running, and so on.
         */
        CANNOT_EXECUTE,
        /**
         * The command ran but rejected its arguments, so the help was shown.
         */
        BAD_ARGS
    }
}
