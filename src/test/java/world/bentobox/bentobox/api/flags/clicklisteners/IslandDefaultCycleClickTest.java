package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
class IslandDefaultCycleClickTest extends RanksManagerTestSetup {

    private IslandDefaultCycleClick listener;
    @Mock
    private Panel panel;
    @Mock
    private User user;
    private Flag flag;
    @Mock
    private GameModeAddon addon;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(Mockito.any())).thenReturn(optionalAddon);

        listener = new IslandDefaultCycleClick("test");

        // Panel
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        when(panel.getWorld()).thenReturn(Optional.of(world));

        // User
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getPlayer()).thenReturn(mock(Player.class));
        when(user.isOp()).thenReturn(false);

        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        // Flags Manager
        FlagsManager flagsMgr = mock(FlagsManager.class);
        flag = mock(Flag.class);
        when(flag.getDefaultRank()).thenReturn(RanksManager.MEMBER_RANK);
        when(flag.getID()).thenReturn("test");

        when(flagsMgr.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(flagsMgr);

        // RanksManager
        mockedRanksManager.when(RanksManager::getInstance).thenReturn(rm);
        when(rm.getRankUpValue(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.OWNER_RANK);
        when(rm.getRankDownValue(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.TRUSTED_RANK);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    void testOnClickNoPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        when(user.isOp()).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.set-world-defaults");
        verify(addon, never()).saveWorldSettings();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    void testOnClickLeftClick() {
        when(user.hasPermission(anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        // Should save world settings
        verify(addon).saveWorldSettings();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    void testOnClickRightClick() {
        when(user.hasPermission(anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.RIGHT, 0);
        // Should save world settings
        verify(addon).saveWorldSettings();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    void testOnClickOpPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        when(user.isOp()).thenReturn(true);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        // Op should be allowed, so save world settings
        verify(addon).saveWorldSettings();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    void testOnClickNoPanelWorld() {
        when(panel.getWorld()).thenReturn(Optional.empty());
        // Should not throw, just return true
        assertTrue(listener.onClick(panel, user, ClickType.LEFT, 0));
        verify(addon, never()).saveWorldSettings();
    }

    /**
     * Test for {@link IslandDefaultCycleClick#onClick(Panel, User, ClickType, int)} with middle click
     */
    @Test
    void testOnClickMiddleClick() {
        when(user.hasPermission(anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.MIDDLE, 0);
        // Middle click should not trigger any save
        verify(addon, never()).saveWorldSettings();
    }
}
