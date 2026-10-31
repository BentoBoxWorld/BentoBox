package world.bentobox.bentobox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import world.bentobox.bentobox.BStats.CommandFailure;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * Tests the accumulation of command failure statistics and the world size measurement.
 *
 * @author tastybento
 */
class BStatsTest {

    private BStats bStats;
    private BentoBox plugin;

    @BeforeEach
    public void setUp() {
        plugin = mock(BentoBox.class);
        bStats = new BStats(plugin);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> field(String name) throws ReflectiveOperationException {
        Field f = BStats.class.getDeclaredField(name);
        f.setAccessible(true);
        return (Map<String, Integer>) f.get(bStats);
    }

    @Test
    void testRecordCommandFailureCountsByType() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.kick");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.team.invite");

        Map<String, Integer> failures = field("commandFailures");
        assertEquals(2, failures.size());
        assertEquals(2, failures.get("no_permission"));
        assertEquals(1, failures.get("bad_args"));
    }

    @Test
    void testRecordCommandFailureCountsByCommand() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.go");

        Map<String, Integer> paths = field("commandFailurePaths");
        assertEquals(2, paths.size());
        assertEquals(2, paths.get("bskyblock.island.team.invite"));
        assertEquals(1, paths.get("bskyblock.island.go"));
    }

    /**
     * A badly behaved addon with generated command names must not be able to grow
     * the map without limit between submissions.
     */
    @Test
    void testRecordCommandFailureCapsDistinctCommands() throws ReflectiveOperationException {
        for (int i = 0; i < 500; i++) {
            bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "addon.command" + i);
        }
        Map<String, Integer> paths = field("commandFailurePaths");
        assertEquals(200, paths.size());
        assertNull(paths.get("addon.command200"));

        // Commands already being tracked keep counting once the cap is reached
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "addon.command0");
        assertEquals(2, paths.get("addon.command0"));
        assertEquals(200, paths.size());

        // Every failure is still counted by type, capped or not
        assertEquals(501, field("commandFailures").get("bad_args"));
    }

    @Test
    void testRecordCommandFailureNoFailures() throws ReflectiveOperationException {
        assertTrue(field("commandFailures").isEmpty());
        assertTrue(field("commandFailurePaths").isEmpty());
        assertTrue(field("commandFailureReasons").isEmpty());
    }

    @Test
    void testRecordCommandFailureCountsByReason() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.team.kick",
                "general.errors.no-team");
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.team.invite",
                "general.errors.no-team");
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.go",
                "general.errors.no-island");

        Map<String, Integer> reasons = field("commandFailureReasons");
        assertEquals(2, reasons.size());
        assertEquals(2, reasons.get("general.errors.no-team"));
        assertEquals(1, reasons.get("general.errors.no-island"));
    }

    /**
     * bStats data is public, so only something shaped like a locale key may be
     * sent as a reason. Translated text, player names and the like become "unknown".
     */
    @Test
    void testSanitizeReason() {
        assertEquals("general.errors.no-island", BStats.sanitizeReason("general.errors.no-island"));
        assertEquals("aoneblock.commands.admin.setcount.set", BStats.sanitizeReason("aoneblock.commands.admin.setcount.set"));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason(null));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason(""));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason("Steve"));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason("§cYou cannot visit Steve's island."));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason("general.errors. no-island"));
        assertEquals(BStats.UNKNOWN_REASON, BStats.sanitizeReason("a." + "b".repeat(200)));
    }

    @Test
    void testRecordCommandFailureUnknownReason() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.go");
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.go", "free text here");

        Map<String, Integer> reasons = field("commandFailureReasons");
        assertEquals(1, reasons.size());
        assertEquals(2, reasons.get(BStats.UNKNOWN_REASON));
    }

    /**
     * What is sent is presence, not volume: one server with a player hammering a
     * command must not swamp the chart. The counts still choose which to send.
     */
    @Test
    void testCommandFailurePathsDataReportsPresenceOfBusiest() throws ReflectiveOperationException {
        for (int i = 0; i < 30; i++) {
            bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "addon.command" + i);
        }
        for (int i = 0; i < 100; i++) {
            bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.team.kick");
        }

        Map<String, Integer> data = bStats.commandFailurePathsData();
        assertEquals(20, data.size());
        assertEquals(1, data.get("bskyblock.island.team.kick"));
        assertEquals("bskyblock.island.team.kick", data.keySet().iterator().next());
        assertTrue(data.values().stream().allMatch(v -> v == 1));
        // Sending resets the counts
        assertTrue(field("commandFailurePaths").isEmpty());
    }

    @Test
    void testCommandFailureReasonsDataReportsPresence() throws ReflectiveOperationException {
        for (int i = 0; i < 50; i++) {
            bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.island.team.kick",
                    "general.errors.not-in-team");
        }
        bStats.recordCommandFailure(CommandFailure.CANNOT_EXECUTE, "bskyblock.visit", null);

        Map<String, Integer> data = bStats.commandFailureReasonsData();
        assertEquals(Map.of("general.errors.not-in-team", 1, BStats.UNKNOWN_REASON, 1), data);
        assertTrue(field("commandFailureReasons").isEmpty());
    }

    @Test
    void testWorldSizeBucketBoundaries() {
        long gb = 1_000_000_000L;
        assertEquals("0-1 GB", BStats.worldSizeBucket(0));
        assertEquals("0-1 GB", BStats.worldSizeBucket(gb - 1));
        assertEquals("1-5 GB", BStats.worldSizeBucket(gb));
        assertEquals("1-5 GB", BStats.worldSizeBucket(5 * gb - 1));
        assertEquals("5-10 GB", BStats.worldSizeBucket(5 * gb));
        assertEquals("10-50 GB", BStats.worldSizeBucket(10 * gb));
        assertEquals("50-100 GB", BStats.worldSizeBucket(50 * gb));
        assertEquals("100-500 GB", BStats.worldSizeBucket(100 * gb));
        assertEquals("500-1000 GB", BStats.worldSizeBucket(500 * gb));
        assertEquals("1 TB+", BStats.worldSizeBucket(1000 * gb));
        assertEquals("1 TB+", BStats.worldSizeBucket(1_200_000_000_000L));
    }

    @Test
    void testFolderSizeSumsNestedRegularFiles(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve("level.dat"), new byte[100]);
        Path region = Files.createDirectories(dir.resolve("region"));
        Files.write(region.resolve("r.0.0.mca"), new byte[4096]);
        Files.write(region.resolve("r.0.1.mca"), new byte[8192]);
        Files.createDirectories(dir.resolve("empty"));

        assertEquals(100 + 4096 + 8192, BStats.folderSize(dir));
    }

    @Test
    void testFolderSizeMissingOrEmptyFolder(@TempDir Path dir) {
        assertEquals(0, BStats.folderSize(dir));
        assertEquals(0, BStats.folderSize(dir.resolve("does-not-exist")));
    }

    /**
     * Worlds sharing a folder are counted once, null worlds (no nether or end) are
     * skipped, and the result lands in the cache the chart reads.
     */
    @Test
    void testMeasureWorldSize(@TempDir Path dir) throws IOException {
        Path over = Files.createDirectories(dir.resolve("bskyblock_world"));
        Files.write(over.resolve("a.mca"), new byte[1000]);
        Path nether = Files.createDirectories(dir.resolve("bskyblock_world_nether"));
        Files.write(nether.resolve("b.mca"), new byte[500]);

        World overWorld = mock(World.class);
        when(overWorld.getWorldFolder()).thenReturn(over.toFile());
        World netherWorld = mock(World.class);
        when(netherWorld.getWorldFolder()).thenReturn(nether.toFile());

        GameModeAddon gm1 = mock(GameModeAddon.class);
        when(gm1.getOverWorld()).thenReturn(overWorld);
        when(gm1.getNetherWorld()).thenReturn(netherWorld);
        when(gm1.getEndWorld()).thenReturn(null);
        // A second game mode pointing at the same overworld folder must not double count
        GameModeAddon gm2 = mock(GameModeAddon.class);
        when(gm2.getOverWorld()).thenReturn(overWorld);

        AddonsManager am = mock(AddonsManager.class);
        when(am.getGameModeAddons()).thenReturn(List.of(gm1, gm2));
        when(plugin.getAddonsManager()).thenReturn(am);

        assertNull(bStats.getWorldSizeBucket());
        bStats.measureWorldSize();
        assertEquals("0-1 GB", bStats.getWorldSizeBucket());
    }

    @Test
    void testMeasureWorldSizeNoGameModes() {
        AddonsManager am = mock(AddonsManager.class);
        when(am.getGameModeAddons()).thenReturn(List.of());
        when(plugin.getAddonsManager()).thenReturn(am);

        bStats.measureWorldSize();
        assertEquals("0-1 GB", bStats.getWorldSizeBucket());
    }
}
