package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.managers.island.IslandGrid;

/**
 * Tests for {@link AdminPurgeCommand}.
 *
 * <p>Since 3.15.0 the top-level purge command does a region-files purge
 * (formerly {@code /bbox admin purge regions}). Tests drive it through the
 * async scheduler mock and assert against the real {@link PurgeRegionsService}.
 */
class AdminPurgeCommandTest extends CommonTestSetup {

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

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

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

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(ac.getWorld()).thenReturn(world);
        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        when(iwm.isNetherGenerate(world)).thenReturn(false);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isEndGenerate(world)).thenReturn(false);
        when(iwm.isEndIslands(world)).thenReturn(false);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.getNetherWorld(world)).thenReturn(null);
        when(iwm.getEndWorld(world)).thenReturn(null);

        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("PlayerName");

        when(im.getIslandCache()).thenReturn(islandCache);
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        when(world.getWorldFolder()).thenReturn(tempDir.toFile());

        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));

        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getAddonByName("Level")).thenReturn(Optional.empty());

        when(plugin.getPurgeRegionsService()).thenReturn(new PurgeRegionsService(plugin));

        apc = new AdminPurgeCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSetup() {
        assertEquals("admin.purge", apc.getPermission());
        assertFalse(apc.isOnlyPlayer());
        assertEquals("commands.admin.purge.parameters", apc.getParameters());
        assertEquals("commands.admin.purge.description", apc.getDescription());
        // 4 explicit subcommands (unowned, protect, age-regions, deleted) + help
        assertEquals(5, apc.getSubCommands().size());
    }

    @Test
    void testCanExecuteEmptyArgs() {
        assertFalse(apc.canExecute(user, "purge", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.help.header"), any(), any());
    }

    @Test
    void testCanExecuteWithArgs() {
        assertTrue(apc.canExecute(user, "purge", List.of("10")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanumber", "0", "-3"})
    void testExecuteInvalidDays(String arg) {
        assertFalse(apc.execute(user, "purge", List.of(arg)));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
    }

    @Test
    void testExecuteNullIslandGrid() {
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteEmptyGrid() {
        wireEmptyGrid();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteNoRegionFiles() throws IOException {
        wireEmptyGrid();
        Files.createDirectories(tempDir.resolve("region"));

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteOldRegionFileNoIslands() throws IOException {
        wireEmptyGrid();
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "0");
        verify(user).sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, "bsb");
    }

    @Test
    void testExecuteConfirmDeletesRegions() throws IOException {
        wireEmptyGrid();
        Path regionFile = tempDir.resolve("region").resolve("r.0.0.mca");
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, "bsb");

        assertTrue(apc.execute(user, "purge", List.of("confirm")));
        verify(user).sendMessage("general.success");
        assertFalse(regionFile.toFile().exists(), "Region file should have been deleted");
    }

    @Test
    void testExecuteConfirmWithoutPriorScan() {
        assertFalse(apc.execute(user, "purge", List.of("confirm")));
        verify(user).sendMessage("commands.admin.purge.days-one-or-more");
        verify(user, never()).sendMessage("general.success");
    }

    @Test
    void testExecuteRecentRegionFileExcluded() throws IOException {
        wireEmptyGrid();
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        File regionFile = regionDir.resolve("r.0.0.mca").toFile();

        byte[] data = new byte[8192];
        int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < 1024; i++) {
            int offset = 4096 + i * 4;
            data[offset]     = (byte) (nowSeconds >> 24);
            data[offset + 1] = (byte) (nowSeconds >> 16);
            data[offset + 2] = (byte) (nowSeconds >> 8);
            data[offset + 3] = (byte)  nowSeconds;
        }
        Files.write(regionFile.toPath(), data);

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteIslandWithRecentLoginIsExcluded() throws IOException {
        UUID ownerUUID = wireIsland("island-1", false, false, false);
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(System.currentTimeMillis());
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteSpawnIslandNotPurged() throws IOException {
        UUID ownerUUID = wireIsland("island-spawn", false, false, true);
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecutePurgeProtectedIslandNotPurged() throws IOException {
        UUID ownerUUID = wireIsland("island-protected", false, true, false);
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    @Test
    void testExecuteDeletableIslandIncluded() throws IOException {
        wireIsland("island-deletable", true, false, false);
        createRegionFile();

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "1");
        verify(user).sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, "bsb");
    }

    @Test
    void testExecuteConfirmDeletesPlayerData() throws IOException {
        UUID ownerUUID = wireIsland("island-deletable", true, false, false);
        when(im.getIslands(world, ownerUUID)).thenReturn(List.of(island));

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.isOp()).thenReturn(false);
        when(offlinePlayer.getLastSeen()).thenReturn(0L);
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(ownerUUID)).thenReturn(offlinePlayer);
        when(pm.getLastLoginTimestamp(ownerUUID)).thenReturn(0L);

        when(im.deleteIslandId("island-deletable")).thenReturn(true);

        createRegionFile();
        Path playerDataDir = Files.createDirectories(tempDir.resolve("playerdata"));
        Path playerFile = playerDataDir.resolve(ownerUUID + ".dat");
        Files.createFile(playerFile);

        assertTrue(apc.execute(user, "purge", List.of("10")));
        verify(user).sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, "bsb");

        assertTrue(apc.execute(user, "purge", List.of("confirm")));
        verify(user).sendMessage("general.success");
        verify(im).deleteIslandId("island-deletable");
        assertFalse(playerFile.toFile().exists(), "Player data file should have been deleted");
    }

    /**
     * Wires the shared {@code island} mock with the given id and flags, puts it
     * in an IslandGrid at (0,0), and returns the generated owner UUID.
     */
    private UUID wireIsland(String id, boolean deletable, boolean purgeProtected, boolean spawn) {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn(id);
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isOwned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(deletable);
        when(island.isPurgeProtected()).thenReturn(purgeProtected);
        when(island.isSpawn()).thenReturn(spawn);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);

        IslandGrid.IslandData data = new IslandGrid.IslandData(id, 0, 0, 100);
        Collection<IslandGrid.IslandData> islandList = List.of(data);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(islandList);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById(id)).thenReturn(Optional.of(island));
        return ownerUUID;
    }

    private void createRegionFile() throws IOException {
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));
    }

    private void wireEmptyGrid() {
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
    }
}
