package world.bentobox.bentobox.api.commands.admin.purge;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.WhiteBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * @author Poslovitch
 *
 */
public class AdminPurgeUnownedCommandTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;

    private AdminPurgeCommand apc;
    private AdminPurgeUnownedCommand apuc;
    @Mock
    private Addon addon;
    @Mock
    private Island island;
    @Mock
    private World world;

    /**
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Set up plugin
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(ac.getWorld()).thenReturn(world);

        when(ac.getAddon()).thenReturn(addon);
        when(ac.getTopLabel()).thenReturn("bsb");

        // Island manager
        when(plugin.getIslands()).thenReturn(im);
        // No islands by default
        when(im.getIslands()).thenReturn(Collections.emptyList());

        // IWM
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.isSpawn()).thenReturn(false);
        when(island.isPurgeProtected()).thenReturn(false);
        when(island.isOwned()).thenReturn(true); // Default owned
        when(island.isUnowned()).thenReturn(false);

        // Command
        apc = new AdminPurgeCommand(ac);
        apuc = new AdminPurgeUnownedCommand(apc);
    }

    @AfterEach
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Makes sure no spawn islands are purged whatsoever
     */
    @Test
    public void testNoPurgeIfIslandIsSpawn() {
        when(island.isSpawn()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.admin.purge.unowned.unowned-islands"), eq("[number]"), eq("0"));
    }

    @Test
    public void testNoPurgeIfIslandIsOwned() {
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.admin.purge.unowned.unowned-islands"), eq("[number]"), eq("0"));
    }

    //@Disabled("unable to mock CompositeCommand#askConfirmation()")
    @Test
    public void testPurgeIfIslandIsUnowned() {
        when(island.isOwned()).thenReturn(false);
        when(island.isUnowned()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.admin.purge.unowned.unowned-islands"), eq("[number]"), eq("1"));
    }

    @Test
    public void testNoPurgeIfIslandIsPurgeProtected() {
        when(island.isPurgeProtected()).thenReturn(true);
        when(im.getIslands()).thenReturn(Collections.singleton(island));
        assertTrue(apuc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.admin.purge.unowned.unowned-islands"), eq("[number]"), eq("0"));
    }
}
