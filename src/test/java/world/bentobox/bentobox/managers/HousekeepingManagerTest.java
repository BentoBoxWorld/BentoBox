package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PurgeRegionsService.FilterStats;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * Tests for {@link HousekeepingManager}.
 *
 * <p>Focus areas:
 * <ol>
 *   <li><b>State persistence</b> — the YAML file round-trips both
 *       {@code lastAgeRunMillis} and {@code lastDeletedRunMillis}.</li>
 *   <li><b>Legacy migration</b> — an existing state file written by the
 *       previous single-cycle implementation (only {@code lastRunMillis})
 *       is adopted as the age-cycle timestamp so existing installs don't
 *       reset their schedule on upgrade.</li>
 *   <li><b>Dual cycle dispatch</b> — the hourly check decides which
 *       cycle(s) to run based on the independent interval settings, and
 *       correctly skips when neither is due or the feature is disabled.</li>
 * </ol>
 */
class HousekeepingManagerTest extends CommonTestSetup {

    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private AddonsManager addonsManager;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private AddonDescription addonDescription;
    @Mock
    private PurgeRegionsService purgeService;

    @TempDir
    Path tempDir;

    private Settings settings;
    private File stateFile;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Point the plugin data folder at the temp dir so housekeeping.yml
        // lives in an isolated location per test.
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        stateFile = new File(new File(tempDir.toFile(), "database"), "housekeeping.yml");

        // Real settings with test-specific overrides
        settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Scheduler: run tasks inline so async cycles become synchronous.
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return null;
        });
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return null;
        });
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        mockedBukkit.when(Bukkit::getWorlds).thenReturn(Collections.emptyList());

        // Addons: single gamemode with a single overworld
        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getGameModeAddons()).thenReturn(List.of(gameMode));
        when(gameMode.getOverWorld()).thenReturn(world);
        when(gameMode.getDescription()).thenReturn(addonDescription);
        when(addonDescription.getName()).thenReturn("TestMode");

        when(plugin.getPurgeRegionsService()).thenReturn(purgeService);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * An install with no prior state file starts both timestamps at zero.
     */
    @Test
    void testLoadStateNoPriorFile() {
        HousekeepingManager hm = new HousekeepingManager(plugin);
        assertEquals(0L, hm.getLastAgeRunMillis());
        assertEquals(0L, hm.getLastDeletedRunMillis());
    }

    /**
     * Legacy state files from the previous single-cycle implementation wrote
     * only {@code lastRunMillis}. The new manager must adopt it as the
     * age-cycle timestamp so upgrades don't reset the schedule. The deleted
     * cycle starts from scratch.
     */
    @Test
    void testLoadStateMigratesLegacyKey() throws Exception {
        Files.createDirectories(stateFile.getParentFile().toPath());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("lastRunMillis", 1700000000000L);
        yaml.save(stateFile);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        assertEquals(1700000000000L, hm.getLastAgeRunMillis(),
                "legacy lastRunMillis should be adopted as age-cycle timestamp");
        assertEquals(0L, hm.getLastDeletedRunMillis());
    }

    /**
     * When both new keys are present the legacy key is ignored even if it's
     * still in the file.
     */
    @Test
    void testLoadStatePrefersNewKeysOverLegacy() throws Exception {
        Files.createDirectories(stateFile.getParentFile().toPath());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("lastRunMillis", 1000L);          // legacy — ignored
        yaml.set("lastAgeRunMillis", 2000L);       // new
        yaml.set("lastDeletedRunMillis", 3000L);   // new
        yaml.save(stateFile);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        assertEquals(2000L, hm.getLastAgeRunMillis());
        assertEquals(3000L, hm.getLastDeletedRunMillis());
    }

    /**
     * Running a cycle must persist both timestamps to the YAML file so a
     * restart doesn't lose either cadence.
     */
    @Test
    void testSaveStateRoundTripsBothKeys() {
        settings.setHousekeepingAgeEnabled(true);
        settings.setHousekeepingIntervalDays(1);
        settings.setHousekeepingRegionAgeDays(30);
        settings.setHousekeepingDeletedIntervalHours(1);

        when(purgeService.scan(eq(world), anyInt())).thenReturn(emptyScan(30));
        when(purgeService.scanDeleted(world)).thenReturn(emptyScan(0));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        hm.runNow();

        assertTrue(hm.getLastAgeRunMillis() > 0, "age cycle timestamp should be set");
        assertTrue(hm.getLastDeletedRunMillis() > 0, "deleted cycle timestamp should be set");

        // Read back from disk with a second manager instance to prove the
        // state is actually persisted, not just in-memory.
        HousekeepingManager reread = new HousekeepingManager(plugin);
        assertEquals(hm.getLastAgeRunMillis(), reread.getLastAgeRunMillis());
        assertEquals(hm.getLastDeletedRunMillis(), reread.getLastDeletedRunMillis());
    }

    // ------------------------------------------------------------------
    // Cycle dispatch
    // ------------------------------------------------------------------

    /**
     * When the feature is disabled both cycles are skipped regardless of
     * what {@code runNow} does with the schedule.
     */
    @Test
    void testDisabledFeatureSkipsAllCycles() throws Exception {
        settings.setHousekeepingDeletedEnabled(false);
        settings.setHousekeepingAgeEnabled(false);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        // Invoke the internal hourly check via reflection so we go through
        // the enabled gate (runNow bypasses that gate).
        invokeCheckAndMaybeRun(hm);

        verify(purgeService, never()).scan(any(), anyInt());
        verify(purgeService, never()).scanDeleted(any());
    }

    /**
     * {@code runNow()} fires both cycles unconditionally (subject to the
     * enabled flag) and dispatches them to the service.
     */
    @Test
    void testRunNowDispatchesBothCycles() {
        settings.setHousekeepingAgeEnabled(true);
        settings.setHousekeepingRegionAgeDays(30);

        when(purgeService.scan(eq(world), eq(30))).thenReturn(emptyScan(30));
        when(purgeService.scanDeleted(world)).thenReturn(emptyScan(0));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        hm.runNow();

        verify(purgeService, times(1)).scan(world, 30);
        verify(purgeService, times(1)).scanDeleted(world);
    }

    /**
     * When the deleted interval is 0 the hourly check only runs the age
     * cycle — the deleted cycle is effectively disabled.
     */
    @Test
    void testDeletedIntervalZeroDisablesDeletedCycle() throws Exception {
        settings.setHousekeepingAgeEnabled(true);
        settings.setHousekeepingIntervalDays(1);
        settings.setHousekeepingRegionAgeDays(30);
        settings.setHousekeepingDeletedIntervalHours(0); // disabled

        when(purgeService.scan(eq(world), eq(30))).thenReturn(emptyScan(30));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeCheckAndMaybeRun(hm);

        verify(purgeService, times(1)).scan(world, 30);
        verify(purgeService, never()).scanDeleted(any());
    }

    /**
     * When the age interval is 0 only the deleted cycle runs.
     */
    @Test
    void testAgeIntervalZeroDisablesAgeCycle() throws Exception {
        settings.setHousekeepingIntervalDays(0); // disabled
        settings.setHousekeepingDeletedIntervalHours(1);

        when(purgeService.scanDeleted(world)).thenReturn(emptyScan(0));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeCheckAndMaybeRun(hm);

        verify(purgeService, never()).scan(any(), anyInt());
        verify(purgeService, times(1)).scanDeleted(world);
    }

    /**
     * If both cycles ran recently (last-run timestamps inside their intervals)
     * the hourly check does nothing.
     */
    @Test
    void testBothCyclesRecentlyRunIsNoop() throws Exception {
        settings.setHousekeepingIntervalDays(30);
        settings.setHousekeepingDeletedIntervalHours(24);

        // Pre-populate the state file so both timestamps are "just now".
        long now = System.currentTimeMillis();
        Files.createDirectories(stateFile.getParentFile().toPath());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("lastAgeRunMillis", now);
        yaml.set("lastDeletedRunMillis", now);
        yaml.save(stateFile);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeCheckAndMaybeRun(hm);

        verify(purgeService, never()).scan(any(), anyInt());
        verify(purgeService, never()).scanDeleted(any());
    }

    /**
     * If only the deleted cycle has aged past its interval, only it runs —
     * the age cycle is left alone.
     */
    @Test
    void testOnlyDeletedCycleDueDispatchesDeletedOnly() throws Exception {
        settings.setHousekeepingIntervalDays(30);
        settings.setHousekeepingDeletedIntervalHours(24);

        long now = System.currentTimeMillis();
        long twoHoursAgo = now - TimeUnit.DAYS.toMillis(2);
        Files.createDirectories(stateFile.getParentFile().toPath());
        YamlConfiguration yaml = new YamlConfiguration();
        // Age cycle ran 1 hour ago (<< 30d interval, not due).
        yaml.set("lastAgeRunMillis", now - TimeUnit.HOURS.toMillis(1));
        // Deleted cycle ran 2 days ago (>= 24h interval, due).
        yaml.set("lastDeletedRunMillis", twoHoursAgo);
        yaml.save(stateFile);

        when(purgeService.scanDeleted(world)).thenReturn(emptyScan(0));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeCheckAndMaybeRun(hm);

        verify(purgeService, never()).scan(any(), anyInt());
        verify(purgeService, times(1)).scanDeleted(world);
    }

    // ------------------------------------------------------------------
    // World save gating
    // ------------------------------------------------------------------

    /**
     * Folia cannot delete region files online: {@code scanDeleted} works from
     * the in-memory island cache and {@code delete} only writes the deferred
     * manifest. A deleted-only cycle therefore has nothing to protect, so the
     * all-world save must not run — while the scan and defer must.
     */
    @Test
    void testFoliaDeletedOnlySkipsWorldSaveButStillDefers() throws Exception {
        PurgeScanResult scan = oneRegionScan(0);
        when(purgeService.scanDeleted(world)).thenReturn(scan);
        when(purgeService.defer(scan)).thenReturn(true);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        try (MockedStatic<TaskScheduler> ts = foliaInline()) {
            invokeRunNow(hm, false, true);
            ts.verify(() -> TaskScheduler.global(any(), any()), never());
        }

        verify(plugin, never()).log(SAVE_STARTED);
        verify(purgeService, times(1)).scanDeleted(world);
        verify(purgeService, times(1)).defer(scan);
        verify(purgeService, never()).delete(any());
    }

    /**
     * A manifest that could not be written is a failure, not a successful
     * cycle: the deleted timestamp must stay untouched and no state is saved.
     */
    @Test
    void testFoliaDeferFailureDoesNotAdvanceDeletedTimestamp() throws Exception {
        PurgeScanResult scan = oneRegionScan(0);
        when(purgeService.scanDeleted(world)).thenReturn(scan);
        when(purgeService.defer(scan)).thenReturn(false);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        try (MockedStatic<TaskScheduler> ts = foliaInline()) {
            invokeRunNow(hm, false, true);
        }

        verify(purgeService, times(1)).defer(scan);
        assertEquals(0L, hm.getLastDeletedRunMillis(), "refused manifest must not look like success");
        assertFalse(stateFile.exists(), "no success state should be persisted");
        verify(plugin).logWarning(
                "Housekeeping deleted sweep: deferred; last-run timestamp was not advanced");
    }

    /**
     * The age sweep reads on-disk region timestamps, so Folia still needs the
     * all-world save before it runs.
     */
    @Test
    void testFoliaAgeCycleStillSavesWorlds() throws Exception {
        settings.setHousekeepingRegionAgeDays(30);
        PurgeScanResult scan = oneRegionScan(30);
        when(purgeService.scan(world, 30)).thenReturn(scan);
        when(purgeService.defer(scan)).thenReturn(true);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        try (MockedStatic<TaskScheduler> ts = foliaInline()) {
            invokeRunNow(hm, true, false);
        }

        verify(plugin).log(SAVE_STARTED);
        verify(plugin).log("Housekeeping: world save complete");
        verify(purgeService, times(1)).scan(world, 30);
        verify(purgeService, times(1)).defer(scan);
    }

    /**
     * Non-Folia deleted sweeps really delete region files, so the save must
     * still happen — and it must happen before the delete.
     */
    @Test
    void testPaperDeletedOnlyStillSavesWorldsBeforeDelete() throws Exception {
        PurgeScanResult scan = oneRegionScan(0);
        when(purgeService.scanDeleted(world)).thenReturn(scan);
        when(purgeService.delete(scan)).thenReturn(true);

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeRunNow(hm, false, true);

        var order = inOrder(plugin, purgeService);
        order.verify(plugin).log(SAVE_STARTED);
        order.verify(purgeService).delete(scan);
        verify(purgeService, never()).defer(any());
    }

    /**
     * A failed save aborts the cycle before any destructive scan or delete,
     * and the in-progress guard is released.
     */
    @Test
    void testSaveFailureAbortsDeletedCycle() throws Exception {
        mockedBukkit.when(Bukkit::getWorlds).thenThrow(new IllegalStateException("save exploded"));

        HousekeepingManager hm = new HousekeepingManager(plugin);
        invokeRunNow(hm, false, true);

        verify(plugin).log(SAVE_STARTED);
        verify(purgeService, never()).scanDeleted(any());
        verify(purgeService, never()).delete(any());
        assertEquals(0L, hm.getLastDeletedRunMillis());
        assertFalse(hm.isInProgress(), "inProgress must be reset after the aborted cycle");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static final String SAVE_STARTED = "Housekeeping: saving all worlds before purge...";

    /** One deletable region; enough for the manager to treat the scan as non-empty. */
    private PurgeScanResult oneRegionScan(int days) {
        return new PurgeScanResult(world, days,
                Map.of(new Pair<>(0, 0), Set.of("island-1")),
                false, false, new FilterStats(0, 0, 0, 0), Set.of());
    }

    /** Folia mode with both scheduler hops executed inline on the test thread. */
    private static MockedStatic<TaskScheduler> foliaInline() {
        MockedStatic<TaskScheduler> ts = mockStatic(TaskScheduler.class);
        ts.when(TaskScheduler::isFolia).thenReturn(true);
        ts.when(() -> TaskScheduler.async(any(), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        });
        ts.when(() -> TaskScheduler.global(any(), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        });
        return ts;
    }

    /** Reflective access to the private two-flag {@code runNow} overload. */
    private static void invokeRunNow(HousekeepingManager hm, boolean runAge, boolean runDeleted)
            throws Exception {
        var m = HousekeepingManager.class.getDeclaredMethod("runNow", boolean.class, boolean.class);
        m.setAccessible(true);
        m.invoke(hm, runAge, runDeleted);
    }

    private static PurgeScanResult emptyScan(int days) {
        return new PurgeScanResult(mock(World.class), days, Collections.emptyMap(),
                false, false, new FilterStats(0, 0, 0, 0), Set.of());
    }

    /** Reflective access to the package-private {@code checkAndMaybeRun} so
     *  tests can drive the hourly path without waiting for the scheduler. */
    private static void invokeCheckAndMaybeRun(HousekeepingManager hm) throws Exception {
        var m = HousekeepingManager.class.getDeclaredMethod("checkAndMaybeRun");
        m.setAccessible(true);
        m.invoke(hm);
    }
}
