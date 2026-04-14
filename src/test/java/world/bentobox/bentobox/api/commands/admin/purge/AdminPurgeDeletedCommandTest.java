package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.managers.island.IslandGrid;

/**
 * Tests for {@link AdminPurgeDeletedCommand}.
 *
 * <p>Exercises the command against a real {@link PurgeRegionsService}
 * wired over the mocked plugin, so the scan/filter/delete logic is
 * driven end-to-end through the async scheduler mock.
 */
class AdminPurgeDeletedCommandTest extends CommonTestSetup {

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
    private AdminPurgeDeletedCommand apdc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Run scheduled tasks inline so async/main scheduling collapses into
        // a synchronous call chain for the test.
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
        when(islandCache.getIslands(world)).thenReturn(Collections.emptyList());
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        when(world.getWorldFolder()).thenReturn(tempDir.toFile());

        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));

        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getAddonByName("Level")).thenReturn(Optional.empty());

        when(plugin.getPurgeRegionsService()).thenReturn(new PurgeRegionsService(plugin));

        apc = new AdminPurgeCommand(ac);
        apdc = new AdminPurgeDeletedCommand(apc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * The command is not registered as a listener — it has no event handlers.
     */
    @Test
    void testConstructor() {
        assertEquals("admin.purge.deleted", apdc.getPermission());
    }

    @Test
    void testSetup() {
        assertEquals("admin.purge.deleted", apdc.getPermission());
        assertFalse(apdc.isOnlyPlayer());
        assertEquals("commands.admin.purge.deleted.parameters", apdc.getParameters());
        assertEquals("commands.admin.purge.deleted.description", apdc.getDescription());
    }

    /**
     * canExecute should accept zero arguments — unlike the age-based command,
     * the deleted sweep takes no parameters.
     */
    @Test
    void testCanExecuteNoArgs() {
        assertTrue(apdc.canExecute(user, "deleted", Collections.emptyList()));
    }

    /**
     * With an empty island cache the scan finds nothing and the user is told.
     */
    @Test
    void testExecuteNoIslands() {
        when(islandCache.getIslands(world)).thenReturn(Collections.emptyList());
        IslandGrid grid = mock(IslandGrid.class);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.scanning");
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * A null island grid (world never registered) yields none-found.
     */
    @Test
    void testExecuteNullGrid() {
        when(islandCache.getIslandGrid(world)).thenReturn(null);

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * Non-deletable islands must be ignored by the deleted sweep — no candidate
     * regions, no confirm prompt.
     */
    @Test
    void testExecuteNonDeletableIgnored() {
        when(island.getUniqueId()).thenReturn("island-active");
        when(island.isDeletable()).thenReturn(false);
        when(island.getMinProtectedX()).thenReturn(0);
        when(island.getMaxProtectedX()).thenReturn(100);
        when(island.getMinProtectedZ()).thenReturn(0);
        when(island.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(island));
        IslandGrid grid = mock(IslandGrid.class);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * A lone deletable island's region is surfaced and the confirm prompt fires.
     */
    @Test
    void testExecuteDeletableIslandFound() {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-deletable");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isDeletable()).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);
        // Occupies region r.0.0 (blocks 0..100)
        when(island.getMinProtectedX()).thenReturn(0);
        when(island.getMaxProtectedX()).thenReturn(100);
        when(island.getMinProtectedZ()).thenReturn(0);
        when(island.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(island));
        IslandGrid.IslandData data = new IslandGrid.IslandData("island-deletable", 0, 0, 100);
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(List.of(data));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-deletable")).thenReturn(Optional.of(island));

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, "1");
        verify(user).sendMessage("commands.admin.purge.deleted.confirm", TextVariables.LABEL, "deleted");
    }

    /**
     * A region shared between a deletable and a non-deletable neighbour must
     * be dropped by the strict filter — non-deletable neighbour blocks reap.
     */
    @Test
    void testExecuteStrictFilterBlocksMixedRegion() {
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        // Deletable island straddling r.0.0
        Island deletable = mock(Island.class);
        when(deletable.getUniqueId()).thenReturn("del");
        when(deletable.isDeletable()).thenReturn(true);
        when(deletable.getMemberSet()).thenReturn(ImmutableSet.of(owner1));
        when(deletable.getMinProtectedX()).thenReturn(0);
        when(deletable.getMaxProtectedX()).thenReturn(100);
        when(deletable.getMinProtectedZ()).thenReturn(0);
        when(deletable.getMaxProtectedZ()).thenReturn(100);

        // Active neighbour sharing r.0.0
        Island active = mock(Island.class);
        when(active.getUniqueId()).thenReturn("act");
        when(active.isDeletable()).thenReturn(false);

        when(islandCache.getIslands(world)).thenReturn(List.of(deletable, active));
        IslandGrid grid = mock(IslandGrid.class);
        Collection<IslandGrid.IslandData> inRegion = List.of(
                new IslandGrid.IslandData("del", 0, 0, 100),
                new IslandGrid.IslandData("act", 200, 200, 100));
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(inRegion);
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("del")).thenReturn(Optional.of(deletable));
        when(im.getIslandById("act")).thenReturn(Optional.of(active));

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.none-found");
    }

    /**
     * Confirm after a scan deletes the region file on disk regardless of age.
     * Crucially this file's timestamp is "now" — the age-based sweep would
     * never touch it, but the deleted sweep must because the island row is
     * flagged.
     */
    @Test
    void testExecuteConfirmReapsFreshRegion() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-deletable");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isDeletable()).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);
        when(island.getMinProtectedX()).thenReturn(0);
        when(island.getMaxProtectedX()).thenReturn(100);
        when(island.getMinProtectedZ()).thenReturn(0);
        when(island.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(island));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new IslandGrid.IslandData("island-deletable", 0, 0, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-deletable")).thenReturn(Optional.of(island));
        when(im.deleteIslandId("island-deletable")).thenReturn(true);

        // Build a fresh 8KB .mca with "now" timestamps — age sweep would skip
        // this; deleted sweep must still reap it.
        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Path regionFile = regionDir.resolve("r.0.0.mca");
        byte[] data = new byte[8192];
        int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
        for (int i = 0; i < 1024; i++) {
            int offset = 4096 + i * 4;
            data[offset]     = (byte) (nowSeconds >> 24);
            data[offset + 1] = (byte) (nowSeconds >> 16);
            data[offset + 2] = (byte) (nowSeconds >> 8);
            data[offset + 3] = (byte)  nowSeconds;
        }
        Files.write(regionFile, data);

        // Scan
        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.deleted.confirm", TextVariables.LABEL, "deleted");

        // Confirm
        assertTrue(apdc.execute(user, "deleted", List.of("confirm")));
        verify(user).sendMessage("commands.admin.purge.deleted.deferred");
        assertFalse(regionFile.toFile().exists(), "Fresh region file should be reaped by the deleted sweep");
        // DB row deletion is deferred to shutdown for days==0 (deleted sweep).
        verify(im, never()).deleteIslandId("island-deletable");
    }

    /**
     * Player data files must NOT be touched by the deleted sweep — the active
     * player could still be playing and reaping their .dat would be harmful.
     */
    @Test
    void testExecuteConfirmLeavesPlayerData() throws IOException {
        UUID ownerUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn("island-deletable");
        when(island.getOwner()).thenReturn(ownerUUID);
        when(island.isDeletable()).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(ownerUUID));
        when(island.getCenter()).thenReturn(location);
        when(island.getMinProtectedX()).thenReturn(0);
        when(island.getMaxProtectedX()).thenReturn(100);
        when(island.getMinProtectedZ()).thenReturn(0);
        when(island.getMaxProtectedZ()).thenReturn(100);

        when(islandCache.getIslands(world)).thenReturn(List.of(island));
        IslandGrid grid = mock(IslandGrid.class);
        when(grid.getIslandsInBounds(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new IslandGrid.IslandData("island-deletable", 0, 0, 100)));
        when(islandCache.getIslandGrid(world)).thenReturn(grid);
        when(im.getIslandById("island-deletable")).thenReturn(Optional.of(island));
        when(im.deleteIslandId("island-deletable")).thenReturn(true);

        Path regionDir = Files.createDirectories(tempDir.resolve("region"));
        Files.createFile(regionDir.resolve("r.0.0.mca"));
        Path playerDataDir = Files.createDirectories(tempDir.resolve("playerdata"));
        Path playerFile = playerDataDir.resolve(ownerUUID + ".dat");
        Files.createFile(playerFile);

        assertTrue(apdc.execute(user, "deleted", Collections.emptyList()));
        assertTrue(apdc.execute(user, "deleted", List.of("confirm")));
        verify(user).sendMessage("commands.admin.purge.deleted.deferred");
        assertTrue(playerFile.toFile().exists(),
                "Deleted sweep must NOT remove player data — only the age sweep does");
    }

    /**
     * A confirm before any scan falls through to the scan path (no args ==
     * empty args are equivalent). It should not produce an error.
     */
    @Test
    void testExecuteConfirmWithoutPriorScan() {
        assertTrue(apdc.execute(user, "deleted", List.of("confirm")));
        verify(user).sendMessage("commands.admin.purge.scanning");
    }
}
