package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.util.Pair;

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
 * <p>Each cycle is independently gated: the age sweep is enabled via
 * {@link world.bentobox.bentobox.Settings#isHousekeepingAgeEnabled()} and the
 * deleted sweep via
 * {@link world.bentobox.bentobox.Settings#isHousekeepingDeletedEnabled()}.
 * Both cycles share an {@code inProgress} guard so they never overlap.
 *
 * <p>Last-run timestamps are persisted to
 * {@code <plugin-data-folder>/database/housekeeping.yml} regardless of the
 * configured database backend, so the schedule survives restarts.
 *
 * <p>This manager is destructive by design: it deletes {@code .mca} region
 * files from disk.
 *
 * @since 3.15.0
 */
public class HousekeepingManager {

    private static final String LOG_PREFIX = "Housekeeping ";

    private static final String LEGACY_LAST_RUN_KEY = "lastRunMillis";
    private static final String LAST_AGE_RUN_KEY = "lastAgeRunMillis";
    private static final String LAST_DELETED_RUN_KEY = "lastDeletedRunMillis";
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L * 60L; // 1 hour
    private static final long STARTUP_DELAY_TICKS  = 20L * 60L * 5L;  // 5 minutes
    /** How long an async sweep waits for one main-thread step before giving up. */
    private static final long MAIN_THREAD_TIMEOUT_MINUTES = 2L;
    /** Regions whose loaded chunks are evicted per tick; each is up to 1024 chunks per dimension. */
    private static final int EVICT_REGIONS_PER_TICK = 2;

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
        plugin.log("Housekeeping scheduler started (deleted-sweep="
                + plugin.getSettings().isHousekeepingDeletedEnabled()
                + ", deleted-interval=" + plugin.getSettings().getHousekeepingDeletedIntervalHours() + "h"
                + ", age-sweep=" + plugin.getSettings().isHousekeepingAgeEnabled()
                + ", age-interval=" + plugin.getSettings().getHousekeepingIntervalDays() + "d"
                + ", region-age=" + plugin.getSettings().getHousekeepingRegionAgeDays() + "d"
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
        long now = System.currentTimeMillis();
        boolean ageDue = plugin.getSettings().isHousekeepingAgeEnabled() && isAgeCycleDue(now);
        boolean deletedDue = plugin.getSettings().isHousekeepingDeletedEnabled() && isDeletedCycleDue(now);
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
                // Worlds already saved during this cycle, so a world shared by
                // both sweeps is only ever saved once.
                Set<World> savedWorlds = new HashSet<>();
                if (runAge) {
                    executeAgeCycle(savedWorlds);
                }
                if (runDeleted) {
                    executeDeletedCycle(savedWorlds);
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
    // Main-thread work, spread across ticks (#3018)
    // ---------------------------------------------------------------

    /**
     * Runs {@code task} on the main server thread and blocks the calling
     * (async) thread until it has finished. Each call lands in its own tick,
     * which is what lets the callers below spread their work out instead of
     * stacking it into a single heavy tick.
     *
     * @param task work that must run on the main thread
     * @param what short description used in error messages
     * @return {@code true} if the task ran to completion
     */
    private boolean runOnMainThreadAndWait(Runnable task, String what) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                task.run();
                done.complete(null);
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        });
        try {
            done.get(MAIN_THREAD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            return true;
        } catch (TimeoutException _) {
            plugin.logError("Housekeeping: " + what + " timed out after " + MAIN_THREAD_TIMEOUT_MINUTES
                    + " minutes");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.logError("Housekeeping: " + what + " interrupted: " + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            plugin.logError("Housekeeping: " + what + " failed: " + e.getCause().getMessage());
            return false;
        }
    }

    /**
     * The worlds a sweep of this game mode can touch: its overworld plus the
     * nether and end when the game mode owns island versions of them. This
     * mirrors the dimension gating in {@link PurgeRegionsService}.
     */
    private List<World> gameModeWorlds(World overworld) {
        List<World> worlds = new ArrayList<>();
        worlds.add(overworld);
        IslandWorldManager iwm = plugin.getIWM();
        if (iwm.isNetherGenerate(overworld) && iwm.isNetherIslands(overworld)) {
            World nether = iwm.getNetherWorld(overworld);
            if (nether != null) {
                worlds.add(nether);
            }
        }
        if (iwm.isEndGenerate(overworld) && iwm.isEndIslands(overworld)) {
            World end = iwm.getEndWorld(overworld);
            if (end != null) {
                worlds.add(end);
            }
        }
        return worlds;
    }

    /**
     * Saves the given worlds so the sweep sees their current on-disk state.
     *
     * <p>{@code World.save()} has to run on the main thread and is the most
     * expensive thing housekeeping does there, so it is kept to the minimum:
     * only the worlds a sweep is about to act on are saved (never every world
     * on the server), each at most once per cycle, and one world per tick.
     *
     * @param worlds worlds to save
     * @param savedWorlds worlds already saved during this cycle; updated
     * @return {@code false} if any save failed, in which case the caller must
     *         not go on to purge
     */
    private boolean saveWorlds(List<World> worlds, Set<World> savedWorlds) {
        for (World w : worlds) {
            if (savedWorlds.contains(w)) {
                continue;
            }
            plugin.log("Housekeeping: saving world '" + w.getName() + "' before purge...");
            if (!runOnMainThreadAndWait(w::save, "save of world '" + w.getName() + "'")) {
                plugin.logError("Housekeeping: could not save world '" + w.getName() + "', skipping its purge");
                return false;
            }
            savedWorlds.add(w);
        }
        return true;
    }

    /**
     * Evicts the loaded chunks of the scan's target regions a few regions per
     * tick, so Paper's autosave cannot re-flush them over the region files
     * that are about to be deleted. A large sweep therefore costs many light
     * ticks rather than one heavy one.
     */
    private void evictChunksInBatches(PurgeScanResult scan) {
        List<Pair<Integer, Integer>> regions = new ArrayList<>(scan.deletableRegions().keySet());
        AtomicInteger evicted = new AtomicInteger();
        for (int i = 0; i < regions.size(); i += EVICT_REGIONS_PER_TICK) {
            List<Pair<Integer, Integer>> batch = regions.subList(i,
                    Math.min(i + EVICT_REGIONS_PER_TICK, regions.size()));
            runOnMainThreadAndWait(
                    () -> evicted.addAndGet(plugin.getPurgeRegionsService().evictChunks(scan, batch)),
                    "chunk eviction");
        }
        plugin.log("Purge deleted: evicted " + evicted.get() + " loaded chunk(s) from "
                + regions.size() + " target region(s)");
    }

    // ---------------------------------------------------------------
    // Cycle execution
    // ---------------------------------------------------------------

    private void executeAgeCycle(Set<World> savedWorlds) {
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
        boolean hasErrors = false;
        for (GameModeAddon gm : gameModes) {
            World overworld = gm.getOverWorld();
            if (overworld == null) {
                continue;
            }
            totalWorlds++;
            plugin.log("Housekeeping age sweep: scanning '" + gm.getDescription().getName()
                    + "' world '" + overworld.getName() + "'");
            // The age scan reads region-file timestamps, so in-memory chunk
            // changes have to be on disk before it looks.
            if (!saveWorlds(gameModeWorlds(overworld), savedWorlds)) {
                hasErrors = true;
                continue;
            }
            PurgeScanResult scan = plugin.getPurgeRegionsService().scan(overworld, ageDays);
            int purged = runDeleteIfNonEmpty(scan, overworld, "age sweep");
            if (purged < 0) {
                hasErrors = true;
            } else {
                totalRegionsPurged += purged;
            }
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startMillis);
        plugin.log("Housekeeping age sweep: complete — " + totalWorlds + " world(s) processed, "
                + totalRegionsPurged + " region(s) purged in " + elapsed.toSeconds() + "s");
        if (!hasErrors) {
            lastAgeRunMillis = System.currentTimeMillis();
            saveState();
        } else {
            plugin.logError("Housekeeping age sweep: completed with errors, last-run timestamp not advanced");
        }
    }

    private void executeDeletedCycle(Set<World> savedWorlds) {
        long startMillis = System.currentTimeMillis();
        List<GameModeAddon> gameModes = plugin.getAddonsManager().getGameModeAddons();
        plugin.log("Housekeeping deleted sweep: starting across " + gameModes.size() + " gamemode(s)");

        int totalWorlds = 0;
        int totalRegionsPurged = 0;
        boolean hasErrors = false;
        for (GameModeAddon gm : gameModes) {
            World overworld = gm.getOverWorld();
            if (overworld == null) {
                continue;
            }
            totalWorlds++;
            plugin.log("Housekeeping deleted sweep: scanning '" + gm.getDescription().getName()
                    + "' world '" + overworld.getName() + "'");
            PurgeScanResult scan = plugin.getPurgeRegionsService().scanDeleted(overworld);
            // The deleted scan is driven by the island database, not by what is
            // on disk, so nothing needs saving unless there is something to
            // purge. The usual hourly run finds nothing and so never touches
            // the main thread at all.
            if (!scan.isEmpty()) {
                if (!saveWorlds(gameModeWorlds(overworld), savedWorlds)) {
                    hasErrors = true;
                    continue;
                }
                // Evict in-memory chunks before the async delete, so Paper's
                // autosave can't re-flush them over the deleted region files.
                evictChunksInBatches(scan);
            }
            int purged = runDeleteIfNonEmpty(scan, overworld, "deleted sweep");
            if (purged < 0) {
                hasErrors = true;
            } else {
                totalRegionsPurged += purged;
            }
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startMillis);
        plugin.log("Housekeeping deleted sweep: complete — " + totalWorlds + " world(s) processed, "
                + totalRegionsPurged + " region(s) purged in " + elapsed.toSeconds() + "s");
        if (!hasErrors) {
            lastDeletedRunMillis = System.currentTimeMillis();
            saveState();
        } else {
            plugin.logError("Housekeeping deleted sweep: completed with errors, last-run timestamp not advanced");
        }
    }

    private int runDeleteIfNonEmpty(PurgeScanResult scan, World overworld, String label) {
        if (scan.isEmpty()) {
            plugin.log(LOG_PREFIX + label + ": nothing to purge in " + overworld.getName());
            return 0;
        }
        plugin.log(LOG_PREFIX + label + ": " + scan.deletableRegions().size() + " region(s) and "
                + scan.uniqueIslandCount() + " island(s) eligible in " + overworld.getName());
        boolean ok = plugin.getPurgeRegionsService().delete(scan);
        if (!ok) {
            plugin.logError(LOG_PREFIX + label + ": purge of " + overworld.getName()
                    + " completed with errors");
            return -1;
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
