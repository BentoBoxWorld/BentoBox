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
 * every gamemode overworld on a configurable schedule. Two independent cycles:
 *
 * <ul>
 *   <li><b>Age sweep</b> — runs every {@code interval-days} days and reaps
 *       regions whose .mca files are older than {@code region-age-days}.</li>
 *   <li><b>Deleted sweep</b> — runs every {@code deleted-interval-hours}
 *       hours and reaps regions for any island already flagged as
 *       {@code deletable} (e.g. from {@code /is reset}), ignoring file age.</li>
 * </ul>
 *
 * <p>Both cycles are gated on the single {@code housekeeping.enabled} flag
 * (default OFF) and share an {@code inProgress} guard so they never overlap.
 *
 * <p>Last-run timestamps are persisted to
 * {@code <plugin-data-folder>/database/housekeeping.yml} regardless of the
 * configured database backend, so the schedule survives restarts.
 *
 * <p>This manager is destructive by design: it deletes {@code .mca} region
 * files from disk.
 *
 * @since 3.14.0
 */
public class HousekeepingManager {

    private static final String LEGACY_LAST_RUN_KEY = "lastRunMillis";
    private static final String LAST_AGE_RUN_KEY = "lastAgeRunMillis";
    private static final String LAST_DELETED_RUN_KEY = "lastDeletedRunMillis";
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 60L; // 1 hour
    private static final long STARTUP_DELAY_TICKS  = 20L * 60L * 5L;  // 5 minutes

    private final BentoBox plugin;
    private final File stateFile;
    private volatile long lastAgeRunMillis;
    private volatile long lastDeletedRunMillis;
    private volatile boolean inProgress;
    private BukkitTask scheduledTask;

    public HousekeepingManager(BentoBox plugin) {
        this.plugin = plugin;
        this.stateFile = new File(new File(plugin.getDataFolder(), "database"), "housekeeping.yml");
        loadState();
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
        scheduledTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::checkAndMaybeRun, STARTUP_DELAY_TICKS, CHECK_INTERVAL_TICKS);
        plugin.log("Housekeeping scheduler started (enabled="
                + plugin.getSettings().isHousekeepingEnabled()
                + ", age-interval=" + plugin.getSettings().getHousekeepingIntervalDays() + "d"
                + ", region-age=" + plugin.getSettings().getHousekeepingRegionAgeDays() + "d"
                + ", deleted-interval=" + plugin.getSettings().getHousekeepingDeletedIntervalHours() + "h"
                + ", last-age-run=" + formatTs(lastAgeRunMillis)
                + ", last-deleted-run=" + formatTs(lastDeletedRunMillis) + ")");
    }

    private static String formatTs(long millis) {
        return millis == 0 ? "never" : Instant.ofEpochMilli(millis).toString();
    }

    /**
     * Stops the periodic housekeeping check. Does not clear the last-run
     * timestamps on disk.
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
     * @return wall-clock timestamp (millis) of the last successful age sweep,
     *         or {@code 0} if it has never run.
     */
    public long getLastAgeRunMillis() {
        return lastAgeRunMillis;
    }

    /**
     * @return wall-clock timestamp (millis) of the last successful deleted
     *         sweep, or {@code 0} if it has never run.
     */
    public long getLastDeletedRunMillis() {
        return lastDeletedRunMillis;
    }

    private void checkAndMaybeRun() {
        if (inProgress) {
            return;
        }
        if (!plugin.getSettings().isHousekeepingEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean ageDue = isAgeCycleDue(now);
        boolean deletedDue = isDeletedCycleDue(now);
        if (!ageDue && !deletedDue) {
            return;
        }
        runNow(ageDue, deletedDue);
    }

    private boolean isAgeCycleDue(long now) {
        int intervalDays = plugin.getSettings().getHousekeepingIntervalDays();
        if (intervalDays <= 0) {
            return false;
        }
        long intervalMillis = TimeUnit.DAYS.toMillis(intervalDays);
        return lastAgeRunMillis == 0 || (now - lastAgeRunMillis) >= intervalMillis;
    }

    private boolean isDeletedCycleDue(long now) {
        int intervalHours = plugin.getSettings().getHousekeepingDeletedIntervalHours();
        if (intervalHours <= 0) {
            return false;
        }
        long intervalMillis = TimeUnit.HOURS.toMillis(intervalHours);
        return lastDeletedRunMillis == 0 || (now - lastDeletedRunMillis) >= intervalMillis;
    }

    /**
     * Triggers an immediate housekeeping cycle for both sweeps (respecting
     * the enabled flag but ignoring the interval timers). Runs asynchronously.
     */
    public synchronized void runNow() {
        runNow(true, true);
    }

    private synchronized void runNow(boolean runAge, boolean runDeleted) {
        if (inProgress) {
            plugin.log("Housekeeping: run requested but already in progress, ignoring");
            return;
        }
        if (!runAge && !runDeleted) {
            return;
        }
        inProgress = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Save worlds once per cycle — both sweeps see a consistent
                // on-disk snapshot.
                if (!saveAllWorlds()) {
                    return;
                }
                if (runAge) {
                    executeAgeCycle();
                }
                if (runDeleted) {
                    executeDeletedCycle();
                }
            } catch (Exception e) {
                plugin.logError("Housekeeping: cycle failed: " + e.getMessage());
                plugin.logStacktrace(e);
            } finally {
                inProgress = false;
            }
        });
    }

    // ---------------------------------------------------------------
    // Cycle execution
    // ---------------------------------------------------------------

    private boolean saveAllWorlds() {
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
        try {
            saved.join();
            plugin.log("Housekeeping: world save complete");
            return true;
        } catch (Exception e) {
            plugin.logError("Housekeeping: world save failed: " + e.getMessage());
            return false;
        }
    }

    private void executeAgeCycle() {
        long startMillis = System.currentTimeMillis();
        int ageDays = plugin.getSettings().getHousekeepingRegionAgeDays();
        if (ageDays <= 0) {
            plugin.logError("Housekeeping: region-age-days must be >= 1, skipping age sweep");
            return;
        }
        List<GameModeAddon> gameModes = plugin.getAddonsManager().getGameModeAddons();
        plugin.log("Housekeeping age sweep: starting across " + gameModes.size()
                + " gamemode(s), region-age=" + ageDays + "d");

        int totalWorlds = 0;
        int totalRegionsPurged = 0;
        for (GameModeAddon gm : gameModes) {
            World overworld = gm.getOverWorld();
            if (overworld == null) {
                continue;
            }
            totalWorlds++;
            plugin.log("Housekeeping age sweep: scanning '" + gm.getDescription().getName()
                    + "' world '" + overworld.getName() + "'");
            PurgeScanResult scan = plugin.getPurgeRegionsService().scan(overworld, ageDays);
            totalRegionsPurged += runDeleteIfNonEmpty(scan, overworld, "age sweep");
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startMillis);
        plugin.log("Housekeeping age sweep: complete — " + totalWorlds + " world(s) processed, "
                + totalRegionsPurged + " region(s) purged in " + elapsed.toSeconds() + "s");
        lastAgeRunMillis = System.currentTimeMillis();
        saveState();
    }

    private void executeDeletedCycle() {
        long startMillis = System.currentTimeMillis();
        List<GameModeAddon> gameModes = plugin.getAddonsManager().getGameModeAddons();
        plugin.log("Housekeeping deleted sweep: starting across " + gameModes.size() + " gamemode(s)");

        int totalWorlds = 0;
        int totalRegionsPurged = 0;
        for (GameModeAddon gm : gameModes) {
            World overworld = gm.getOverWorld();
            if (overworld == null) {
                continue;
            }
            totalWorlds++;
            plugin.log("Housekeeping deleted sweep: scanning '" + gm.getDescription().getName()
                    + "' world '" + overworld.getName() + "'");
            PurgeScanResult scan = plugin.getPurgeRegionsService().scanDeleted(overworld);
            // Evict in-memory chunks on the main thread before the async delete,
            // so Paper's autosave can't re-flush them over the deleted region files.
            if (!scan.isEmpty()) {
                evictChunksOnMainThread(scan);
            }
            totalRegionsPurged += runDeleteIfNonEmpty(scan, overworld, "deleted sweep");
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startMillis);
        plugin.log("Housekeeping deleted sweep: complete — " + totalWorlds + " world(s) processed, "
                + totalRegionsPurged + " region(s) purged in " + elapsed.toSeconds() + "s");
        lastDeletedRunMillis = System.currentTimeMillis();
        saveState();
    }

    private void evictChunksOnMainThread(PurgeScanResult scan) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                plugin.getPurgeRegionsService().evictChunks(scan);
                done.complete(null);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        try {
            done.join();
        } catch (Exception e) {
            plugin.logError("Housekeeping: chunk eviction failed: " + e.getMessage());
        }
    }

    private int runDeleteIfNonEmpty(PurgeScanResult scan, World overworld, String label) {
        if (scan.isEmpty()) {
            plugin.log("Housekeeping " + label + ": nothing to purge in " + overworld.getName());
            return 0;
        }
        plugin.log("Housekeeping " + label + ": " + scan.deletableRegions().size() + " region(s) and "
                + scan.uniqueIslandCount() + " island(s) eligible in " + overworld.getName());
        boolean ok = plugin.getPurgeRegionsService().delete(scan);
        if (!ok) {
            plugin.logError("Housekeeping " + label + ": purge of " + overworld.getName()
                    + " completed with errors");
            return 0;
        }
        return scan.deletableRegions().size();
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    private void loadState() {
        if (!stateFile.exists()) {
            lastAgeRunMillis = 0L;
            lastDeletedRunMillis = 0L;
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
            // Migrate legacy single-cycle key: if the new key is absent but
            // the old one is present, adopt it as the age-cycle timestamp.
            if (yaml.contains(LAST_AGE_RUN_KEY)) {
                lastAgeRunMillis = yaml.getLong(LAST_AGE_RUN_KEY, 0L);
            } else {
                lastAgeRunMillis = yaml.getLong(LEGACY_LAST_RUN_KEY, 0L);
            }
            lastDeletedRunMillis = yaml.getLong(LAST_DELETED_RUN_KEY, 0L);
        } catch (Exception e) {
            plugin.logError("Housekeeping: could not read " + stateFile.getAbsolutePath()
                    + ": " + e.getMessage());
            lastAgeRunMillis = 0L;
            lastDeletedRunMillis = 0L;
        }
    }

    private void saveState() {
        try {
            File parent = stateFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.logError("Housekeeping: could not create " + parent.getAbsolutePath());
                return;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(LAST_AGE_RUN_KEY, lastAgeRunMillis);
            yaml.set(LAST_DELETED_RUN_KEY, lastDeletedRunMillis);
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.logError("Housekeeping: could not write " + stateFile.getAbsolutePath()
                    + ": " + e.getMessage());
        }
    }
}
