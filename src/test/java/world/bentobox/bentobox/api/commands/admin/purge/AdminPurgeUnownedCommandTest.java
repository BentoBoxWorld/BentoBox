package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Tests for {@link AdminPurgeUnownedCommand}. The command scans for orphan
 * islands and (on confirmation) soft-deletes them so the region-files purge
 * can reap their {@code .mca} files later. These tests cover the scan phase;
 * the confirmation-triggered soft-delete path is exercised via
 * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland}.
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

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(ac.getWorld()).thenReturn(world);

        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        when(im.getIslands()).thenReturn(Collections.emptyList());

        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

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

    /**
     * Spawn islands must never be flagged deletable.
     */
    @Test
    void testNoPurgeIfIslandIsSpawn() {
        when(island.isSpawn()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * Owned islands must never be flagged deletable.
     */
    @Test
    void testNoPurgeIfIslandIsOwned() {
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }

    /**
     * A genuine orphan gets counted and (later, on confirm) flagged.
     */
    @Test
    void testPurgeIfIslandIsUnowned() {
        when(island.isOwned()).thenReturn(false);
        when(island.isUnowned()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "1");
    }

    /**
     * Purge-protected islands must never be flagged deletable, even if unowned.
     */
    @Test
    void testNoPurgeIfIslandIsPurgeProtected() {
        when(island.isPurgeProtected()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
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
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.purge.unowned.unowned-islands", "[number]", "0");
    }
}
