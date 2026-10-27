package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * Tests for {@link AdminPurgeUnownedCommand}. The command scans for orphan
 * islands and (on confirmation) soft-deletes them so the region-files purge
 * can reap their {@code .mca} files later.
 */
class AdminPurgeUnownedCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;

    private AdminPurgeCommand apc;
    private AdminPurgeUnownedCommand apuc;
    @Mock
    private Addon addon;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(plugin.getCommandsManager()).thenReturn(mock(CommandsManager.class));
        when(ac.getWorld()).thenReturn(world);

        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        when(island.getUniqueId()).thenReturn("island-id");
        when(island.getWorld()).thenReturn(world);
        when(island.isSpawn()).thenReturn(false);
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isOwned()).thenReturn(true);
        when(island.isUnowned()).thenReturn(false);
        when(island.isDeletable()).thenReturn(false);
        when(island.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));

        apc = new AdminPurgeCommand(ac);
        apuc = new AdminPurgeUnownedCommand(apc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Runs execute and flushes the async scan's user-thread callback. */
    private void executeAndFlush() {
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(() -> TaskScheduler.user(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return null;
            });
            assertTrue(apuc.execute(user, "", Collections.emptyList()));
        }
    }

    private void scanReturns(Collection<Island> islands) {
        when(im.getIslandsASync()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>(islands)));
    }

    /**
     * Spawn islands must never be flagged deletable.
     */
    @Test
    void testNoPurgeIfIslandIsSpawn() {
        when(island.isSpawn()).thenReturn(true);
        scanReturns(List.of(island));
        executeAndFlush();
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * Owned islands must never be flagged deletable.
     */
    @Test
    void testNoPurgeIfIslandIsOwned() {
        scanReturns(List.of(island));
        executeAndFlush();
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * A genuine orphan gets counted and (later, on confirm) flagged.
     */
    @Test
    void testPurgeIfIslandIsUnowned() {
        when(island.isOwned()).thenReturn(false);
        when(island.isUnowned()).thenReturn(true);
        scanReturns(List.of(island));
        executeAndFlush();
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "1");
    }

    /**
     * Purge-protected islands must never be flagged deletable, even if unowned.
     */
    @Test
    void testNoPurgeIfIslandIsPurgeProtected() {
        when(island.isPurgeProtected()).thenReturn(true);
        scanReturns(List.of(island));
        executeAndFlush();
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * Islands already flagged deletable must not be counted again — they are
     * already in the queue the regions-purge will drain.
     */
    @Test
    void testNoPurgeIfIslandAlreadyDeletable() {
        when(island.isOwned()).thenReturn(false);
        when(island.isUnowned()).thenReturn(true);
        when(island.isDeletable()).thenReturn(true);
        scanReturns(List.of(island));
        executeAndFlush();
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * If the island was claimed during the confirmation window, confirming must not
     * soft-delete it (that would eventually wipe a live player's region files).
     */
    @Test
    void testConfirmSkipsIslandClaimedMeanwhile() {
        when(island.isOwned()).thenReturn(false);
        when(island.isUnowned()).thenReturn(true);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));
        scanReturns(List.of(island));
        apuc = Mockito.spy(apuc);
        List<Runnable> pending = new ArrayList<>();
        Mockito.doAnswer(call -> {
            pending.add(call.getArgument(1));
            return null;
        }).when(apuc).askConfirmation(any(), any(Runnable.class));

        executeAndFlush();
        // Another admin registered the island while the prompt was open.
        when(island.isOwned()).thenReturn(true);
        pending.getFirst().run();

        verify(im, never()).deleteIsland(any(), anyBoolean(), any());
    }
}
