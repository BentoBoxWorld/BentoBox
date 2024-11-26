package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.FlagWorldSettingChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.mocks.ServerMocks;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class })
public class WorldToggleClickTest {

    @Mock
    private IslandWorldManager iwm;
    private WorldToggleClick listener;
    @Mock
    private Panel panel;
    @Mock
    private User user;
    private Flag flag;
    @Mock
    private GameModeAddon addon;
    @Mock
    private PluginManager pim;
    @Mock
    private World world;

    @Before
    public void setUp() throws Exception {
        ServerMocks.newServer();
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(Mockito.any())).thenReturn(optionalAddon);
        when(plugin.getIWM()).thenReturn(iwm);

        listener = new WorldToggleClick("test");

        // Panel
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        when(panel.getWorld()).thenReturn(Optional.of(world));

        // User
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getPlayer()).thenReturn(mock(Player.class));

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        flag = mock(Flag.class);
        when(flag.isSetForWorld(any())).thenReturn(false);

        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Event
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test for {@link WorldToggleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.world.settings.test");
        verify(addon, never()).saveWorldSettings();
    }

    /**
     * Test for {@link WorldToggleClick#onClick(Panel, User, ClickType, int)}
     */
    @Test
    public void testOnClick() {
        when(user.hasPermission(anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(flag).setSetting(any(), eq(true));
        verify(addon).saveWorldSettings();
        verify(pim).callEvent(any(FlagWorldSettingChangeEvent.class));
    }
}
