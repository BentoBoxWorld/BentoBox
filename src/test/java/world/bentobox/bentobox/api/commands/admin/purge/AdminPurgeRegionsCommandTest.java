package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.managers.island.IslandGrid;

/**
 * Tests for {@link AdminPurgeRegionsCommand}.
 */
public class AdminPurgeRegionsCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private Addon addon;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private IslandCache islandCache;
    @Mock
    private PlayersManager pm;
    @Mock
    private AddonsManager addonsManager;

    @TempDir
    Path tempDir;

    private AdminPurgeCommand apc;
    private AdminPurgeRegionsCommand aprc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Scheduler - run tasks immediately (both async and sync)
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

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(ac.getWorld()).thenReturn(world);
        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        // IWM - no nether/end by default
        when(iwm.isNetherGenerate(world)).thenReturn(false);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isEndGenerate(world)).thenReturn(false);
        when(iwm.isEndIslands(world)).thenReturn(false);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.getNetherWorld(world)).thenReturn(null);
        when(iwm.getEndWorld(world)).thenReturn(null);

        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("PlayerName");

        // Island cache
        when(im.getIslandCache()).thenReturn(islandCache);
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        // World folder points to our temp directory
        when(world.getWorldFolder()).thenReturn(tempDir.toFile());

        // Island
        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));

        // Addons manager (used by canDeleteIsland for Level check)
        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getAddonByName("Level")).thenReturn(Optional.empty());

        // Create commands
        apc = new AdminPurgeCommand(ac);
        aprc = new AdminPurgeRegionsCommand(apc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verify the command registers itself as a Bukkit listener via the addon.
     * AdminPurgeCommand.setup() creates one instance, and setUp() creates a second — so two calls.
     */
    @Test
    public void testConstructor() {
        verify(addon, times(2)).registerListener(any(AdminPurgeRegionsCommand.class));
    }

    /**
     * Verify command metadata set in setup().
     */
    @Test
    public void testSetup() {
        assertEquals("admin.purge.regions", aprc.getPermission());
        assertFalse(aprc.isOnlyPlayer());
        assertEquals("commands.admin.purge.regions.parameters", aprc.getParameters());
        assertEquals("commands.admin.purge.regions.description", aprc.getDescription());
    }

    /**
     * canExecute with no args should show help and return false.
     */
    @Test
    public void testCanExecuteEmptyArgs() {
        assertFalse(aprc.canExecute(user, "regions", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.help.header"), any(), any());
    }

    /**
     * canExecute with a valid argument should return true.
     */
    @Test
    public void testCanExecuteWithArgs() {
        assertTrue(aprc.canExecute(user, "regions", List.of("10")));
        verify(user, never()).sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, "bsb");
    }

    /**
     * A non-numeric argument should produce an error message and return false.
     */
    @Test
    public void testExecuteNotANumber() {
        assertFalse(aprc.execute(user, "regions", List.of("notanumber")));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
    }

    /**
     * Zero days is invalid — must be one or more.
     */
    @Test
    public void testExecuteZeroDays() {
        assertFalse(aprc.execute(user, "regions", List.of("0")));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
    }

    /**
     * Negative days is invalid.
     */
    @Test
    public void testExecuteNegativeDays() {
        assertFalse(aprc.execute(user, "regions", List.of("-3")));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
    }

    /**
     * When the island grid is null (no islands registered for the world),
     * the command should report none found.
     */
    @Test
    public void testExecuteNullIslandGrid() {
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * When the island grid's internal map is null,
     * the command should report none found.
     */
    @Test
    public void testExecuteNullGrid() {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(null);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * When there are no .mca files in the region folder, none-found should be sent.
     */
    @Test
    public void testExecuteNoRegionFiles() throws IOException {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(new TreeMap<>());
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        // Create the region directory but leave it empty
        Files.createDirectories(tempDir.resolve("region"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * When there is an old region file with no associated islands (empty grid),
     * the command should propose deletion and ask for confirmation.
     */
    @Test
    public void testExecuteOldRegionFileNoIslands() throws IOException {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(new TreeMap<>());
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        // Create a small .mca file — getRegionTimestamp() returns 0 for files < 8192 bytes,
        // which is treated as very old (older than any cutoff).
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        // 0 islands found, but the empty region itself is deletable → confirm prompt
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "0");
        verify(user).sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, "regions");
    }

    /**
     * After scanning finds deletable regions, executing "confirm" should delete
     * the region files and send the success message.
     */
    @Test
    public void testExecuteConfirmDeletesRegions() throws IOException {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(new TreeMap<>());
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Path regionFile = regionDir.resolve("r.0.0.mca");
        Files.createFile(regionFile);

        // First execution: scan and find the old empty region
        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, "regions");

        // Second execution: confirm deletion
        assertTrue(aprc.execute(user, "regions", List.of("confirm")));
        verify(user).sendMessage("general.success");
        assertFalse(regionFile.toFile().exists(), "Region file should have been deleted");
    }

    /**
     * If "confirm" is sent before a scan, it should not trigger deletion
     * and should fall through to normal argument parsing (returning false for "confirm"
     * which is not a valid number).
     */
    @Test
    public void testExecuteConfirmWithoutPriorScan() {
        assertFalse(aprc.execute(user, "regions", List.of("confirm")));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
        verify(user, never()).sendMessage("general.success");
    }

    /**
     * A recent region file (newer than the cutoff) should NOT be included in the deletion list.
     * The command should report none found.
     */
    @Test
    public void testExecuteRecentRegionFileExcluded() throws IOException {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(new TreeMap<>());
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        // Create a valid 8192-byte .mca file with a very recent timestamp so it is excluded.
        // The timestamp table occupies the second 4KB block; we'll write a recent Unix timestamp.
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        File regionFile = regionDir.resolve("r.0.0.mca").toFile();

        // Build a minimal 8192-byte region file with current timestamp in all 1024 chunk slots
        byte[] data = new byte[8192];
        // Write current time (in seconds) as big-endian int into each 4-byte slot of the timestamp table
        int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < 1024; i++) {
            int offset = 4096 + i * 4;
            data[offset]     = (byte) (nowSeconds >> 24);
            data[offset + 1] = (byte) (nowSeconds >> 16);
            data[offset + 2] = (byte) (nowSeconds >> 8);
            data[offset + 3] = (byte)  nowSeconds;
        }
        Files.write(regionFile.toPath(), data);

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * Note: {@code canDeleteIsland()} contains an inverted login-check — when a member's last
     * login is &gt;= the cutoff (i.e. they logged in recently), the method returns {@code false}
     * ("can delete") instead of {@code true} ("cannot delete"). This test documents the actual
     * behaviour so the bug is visible and guarded against accidental change.
     *
     * Correct behaviour would be: island whose member logged in recently → excluded from purge.
     */
    @Test
    public void testExecuteIslandWithRecentLoginIsIncludedDueToBug() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-1");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(false);
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isSpawn()).thenReturn(false);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);

        // Island occupies region r.0.0 (minX=0, minZ=0, range=100)
        IslandGrid.IslandData data = new IslandGrid.IslandData("island-1", 0, 0, 100);
        TreeMap<Integer, IslandGrid.IslandData> zMap = new TreeMap<>();
        zMap.put(0, data);
        TreeMap<Integer, TreeMap<Integer, IslandGrid.IslandData>> gridMap = new TreeMap<>();
        gridMap.put(0, zMap);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(gridMap);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-1")).thenReturn(Optional.of(island));

        // Owner logged in recently — should protect the island but due to the inverted
        // login check in canDeleteIsland() it currently does not.
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(System.currentTimeMillis());

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        // BUG: island IS included despite recent login
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "1");
        verify(user).sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, "regions");
    }

    /**
     * Spawn islands must never be purged.
     */
    @Test
    public void testExecuteSpawnIslandNotPurged() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-spawn");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(false);
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isSpawn()).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));

        IslandGrid.IslandData data = new IslandGrid.IslandData("island-spawn", 0, 0, 100);
        TreeMap<Integer, IslandGrid.IslandData> zMap = new TreeMap<>();
        zMap.put(0, data);
        TreeMap<Integer, TreeMap<Integer, IslandGrid.IslandData>> gridMap = new TreeMap<>();
        gridMap.put(0, zMap);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(gridMap);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-spawn")).thenReturn(Optional.of(island));

        // Owner hasn't logged in for a long time
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * Purge-protected islands must never be purged.
     */
    @Test
    public void testExecutePurgeProtectedIslandNotPurged() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-protected");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(false);
        when(island.isPurgeProtected()).thenReturn(true);
        when(island.isSpawn()).thenReturn(false);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));

        IslandGrid.IslandData data = new IslandGrid.IslandData("island-protected", 0, 0, 100);
        TreeMap<Integer, IslandGrid.IslandData> zMap = new TreeMap<>();
        zMap.put(0, data);
        TreeMap<Integer, TreeMap<Integer, IslandGrid.IslandData>> gridMap = new TreeMap<>();
        gridMap.put(0, zMap);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(gridMap);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-protected")).thenReturn(Optional.of(island));

        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * An island marked as deletable should always be eligible regardless of other flags.
     */
    @Test
    public void testExecuteDeletableIslandIncluded() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-deletable");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(true); // deletable flag set
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isSpawn()).thenReturn(false);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);

        IslandGrid.IslandData data = new IslandGrid.IslandData("island-deletable", 0, 0, 100);
        TreeMap<Integer, IslandGrid.IslandData> zMap = new TreeMap<>();
        zMap.put(0, data);
        TreeMap<Integer, TreeMap<Integer, IslandGrid.IslandData>> gridMap = new TreeMap<>();
        gridMap.put(0, zMap);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(gridMap);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-deletable")).thenReturn(Optional.of(island));

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));

        assertTrue(aprc.execute(user, "regions", List.of("10")));
        // The island is deletable → canDeleteIsland returns false → region is kept → confirm prompt
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "1");
        verify(user).sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, "regions");
    }

    /**
     * Deleting a region where the island has members removes their playerdata files
     * when they have no remaining islands and haven't logged in recently.
     */
    @Test
    public void testExecuteConfirmDeletesPlayerData() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-deletable");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(true);
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isSpawn()).thenReturn(false);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);

        IslandGrid.IslandData data = new IslandGrid.IslandData("island-deletable", 0, 0, 100);
        TreeMap<Integer, IslandGrid.IslandData> zMap = new TreeMap<>();
        zMap.put(0, data);
        TreeMap<Integer, TreeMap<Integer, IslandGrid.IslandData>> gridMap = new TreeMap<>();
        gridMap.put(0, zMap);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getGrid()).thenReturn(gridMap);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-deletable")).thenReturn(Optional.of(island));

        // Player has no remaining islands after deletion
        when(im.getIslands(world, ownerUUID)).thenReturn(List.of(island));

        // Player is not op and hasn't logged in recently
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.isOp()).thenReturn(false);
        when(offlinePlayer.getLastSeen()).thenReturn(0L);
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(ownerUUID)).thenReturn(offlinePlayer);
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);

        // Island cache operations
        when(im.deleteIslandId("island-deletable")).thenReturn(true);

        // Create region and playerdata files
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));
        Path playerDataDir = Files.createDirectories(tempDir.resolve("playerdata"));
        Path playerFile = playerDataDir.resolve(ownerUUID + ".dat");
        Files.createFile(playerFile);

        // Scan
        assertTrue(aprc.execute(user, "regions", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, "regions");

        // Confirm
        assertTrue(aprc.execute(user, "regions", List.of("confirm")));
        verify(user).sendMessage("general.success");
        verify(im).deleteIslandId("island-deletable");
        assertFalse(playerFile.toFile().exists(), "Player data file should have been deleted");
    }
}
