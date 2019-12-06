package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock.");
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(Mockito.any())).thenReturn(optionalAddon);
        when(plugin.getIWM()).thenReturn(iwm);

        listener = new WorldToggleClick("test");

        // Panel
        when(panel.getInventory()).thenReturn(mock(Inventory.class));

        // User
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getPlayer()).thenReturn(mock(Player.class));

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        flag = mock(Flag.class);
        when(flag.isSetForWorld(Mockito.any())).thenReturn(false);

        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Event
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testOnClickWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.wrong-world");
        verify(addon, Mockito.never()).saveWorldSettings();
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.world.settings.test");
        verify(addon, Mockito.never()).saveWorldSettings();
    }

    @Test
    public void testOnClick() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(flag).setSetting(Mockito.any(), Mockito.eq(true));
        verify(addon).saveWorldSettings();
        verify(pim).callEvent(any(FlagWorldSettingChangeEvent.class));
    }
}
