package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;

/**
 * Periodic housekeeping: automatically runs the region-files purge against
 * every gamemode overworld on a configurable schedule.
 *
 * <p>Enabled via {@code island.deletion.housekeeping.enabled}. The task runs
 * every {@code interval-days} days (wall-clock, not uptime) and scans for
 * regions older than {@code region-age-days}. Since player resets now
 * orphan islands instead of physically deleting their blocks, this scheduler
 * is how the disk space is eventually reclaimed.
 *
 * <p>Last-run timestamp is persisted to
 * {@code <plugin-data-folder>/database/housekeeping.yml} regardless of the
 * configured database backend, so the schedule survives restarts.
 *
 * <p>This manager is destructive by design: it deletes {@code .mca} region
 * files from disk. Default is OFF.
 *
 * @since 3.14.0
 */
public class HousekeepingManager {

    private static final String LAST_RUN_KEY = "lastRunMillis";
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 60L; // 1 hour
    private static final long STARTUP_DELAY_TICKS  = 20L * 60L * 5L;  // 5 minutes

    private final BentoBox plugin;
    private final File stateFile;
    private volatile long lastRunMillis;
    private volatile boolean inProgress;
    private BukkitTask scheduledTask;

    public HousekeepingManager(BentoBox plugin) {
        this.plugin = plugin;
        this.stateFile = new File(new File(plugin.getDataFolder(), "database"), "housekeeping.yml");
        this.lastRunMillis = loadLastRun();
    }

    // ---------------------------------------------------------------
    // Scheduling
    // ---------------------------------------------------------------

    /**
     * Starts the periodic housekeeping check. Safe to call multiple times —
     * the task is only scheduled once.
     */
    public synchronized void start() {
        if (scheduledTask != null) {
            return;
        }
        // Check hourly; each check runs the purge only if the wall-clock
        // interval since the last run has elapsed and the feature is enabled.
        scheduledTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::checkAndMaybeRun, STARTUP_DELAY_TICKS, CHECK_INTERVAL_TICKS);
        plugin.log("Housekeeping scheduler started (enabled="
                + plugin.getSettings().isHousekeepingEnabled()
                + ", interval=" + plugin.getSettings().getHousekeepingIntervalDays() + "d"
                + ", region-age=" + plugin.getSettings().getHousekeepingRegionAgeDays() + "d"
                + ", last-run=" + (lastRunMillis == 0 ? "never" : Instant.ofEpochMilli(lastRunMillis)) + ")");
    }

    /**
     * Stops the periodic housekeeping check. Does not clear the last-run
     * timestamp on disk.
     */
    public synchronized void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    /**
     * @return {@code true} if a housekeeping run is currently in progress.
     */
    public boolean isInProgress() {
        return inProgress;
    }

    /**
     * @return the wall-clock timestamp (millis) of the last successful run,
     *         or {@code 0} if the task has never run.
     */
    public long getLastRunMillis() {
        return lastRunMillis;
    }

    private void checkAndMaybeRun() {
        if (inProgress) {
            return;
        }
        if (!plugin.getSettings().isHousekeepingEnabled()) {
            return;
        }
        int intervalDays = plugin.getSettings().getHousekeepingIntervalDays();
        if (intervalDays <= 0) {
            plugin.logWarning("Housekeeping: interval-days must be >= 1, skipping run");
            return;
        }
        long intervalMillis = TimeUnit.DAYS.toMillis(intervalDays);
        long now = System.currentTimeMillis();
        if (lastRunMillis != 0 && (now - lastRunMillis) < intervalMillis) {
            return;
        }
        runNow();
    }

    /**
     * Triggers an immediate housekeeping cycle, regardless of the
     * wall-clock interval (but still respecting {@code enabled}).
     * Runs asynchronously.
     */
    public synchronized void runNow() {
        if (inProgress) {
            plugin.log("Housekeeping: run requested but already in progress, ignoring");
            return;
        }
        inProgress = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::executeCycle);
    }

    // ---------------------------------------------------------------
    // Cycle execution
    // ---------------------------------------------------------------

    private void executeCycle() {
        long startMillis = System.currentTimeMillis();
        try {
            int ageDays = plugin.getSettings().getHousekeepingRegionAgeDays();
            if (ageDays <= 0) {
                plugin.logError("Housekeeping: region-age-days must be >= 1, aborting run");
                return;
            }
            List<GameModeAddon> gameModes = plugin.getAddonsManager().getGameModeAddons();
            plugin.log("Housekeeping: starting auto-purge cycle across " + gameModes.size()
                    + " gamemode(s), region-age=" + ageDays + "d");
            // Save worlds up-front so disk state matches memory. World.save()
            // must run on the main thread — hop over and block the async
            // cycle until the save completes.
            plugin.log("Housekeeping: saving all worlds before purge...");
            CompletableFuture<Void> saved = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.getWorlds().forEach(World::save);
                    saved.complete(null);
                } catch (Exception e) {
                    saved.completeExceptionally(e);
                }
            });
            saved.join();
            plugin.log("Housekeeping: world save complete");

            int totalWorlds = 0;
            int totalRegionsPurged = 0;
            for (GameModeAddon gm : gameModes) {
                World overworld = gm.getOverWorld();
                if (overworld == null) {
                    continue;
                }
                totalWorlds++;
                plugin.log("Housekeeping: scanning gamemode '" + gm.getDescription().getName()
                        + "' world '" + overworld.getName() + "'");
                PurgeScanResult scan = plugin.getPurgeRegionsService().scan(overworld, ageDays);
                if (scan.isEmpty()) {
                    plugin.log("Housekeeping: nothing to purge in " + overworld.getName());
                    continue;
                }
                plugin.log("Housekeeping: " + scan.deleteableRegions().size() + " region(s) and "
                        + scan.uniqueIslandCount() + " island(s) eligible in " + overworld.getName());
                boolean ok = plugin.getPurgeRegionsService().delete(scan);
                if (ok) {
                    totalRegionsPurged += scan.deleteableRegions().size();
                } else {
                    plugin.logError("Housekeeping: purge of " + overworld.getName()
                            + " completed with errors");
                }
            }

            Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startMillis);
            plugin.log("Housekeeping: cycle complete — " + totalWorlds + " world(s) processed, "
                    + totalRegionsPurged + " region(s) purged in " + elapsed.toSeconds() + "s");
            lastRunMillis = System.currentTimeMillis();
            saveLastRun();
        } catch (Exception e) {
            plugin.logError("Housekeeping: cycle failed: " + e.getMessage());
            plugin.logStacktrace(e);
        } finally {
            inProgress = false;
        }
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    private long loadLastRun() {
        if (!stateFile.exists()) {
            return 0L;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
            return yaml.getLong(LAST_RUN_KEY, 0L);
        } catch (Exception e) {
            plugin.logError("Housekeeping: could not read " + stateFile.getAbsolutePath()
                    + ": " + e.getMessage());
            return 0L;
        }
    }

    private void saveLastRun() {
        try {
            File parent = stateFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.logError("Housekeeping: could not create " + parent.getAbsolutePath());
                return;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(LAST_RUN_KEY, lastRunMillis);
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.logError("Housekeeping: could not write " + stateFile.getAbsolutePath()
                    + ": " + e.getMessage());
        }
    }
}
