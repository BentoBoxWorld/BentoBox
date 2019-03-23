package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class })
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
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");
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

        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(Mockito.any(), Mockito.eq(user), Mockito.eq(false))).thenReturn(item);
        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);
    }

    @Test
    public void testOnClickWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.wrong-world");
        Mockito.verify(addon, Mockito.never()).saveWorldSettings();
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.world.settings.test");
        Mockito.verify(addon, Mockito.never()).saveWorldSettings();
    }

    @Test
    public void testOnClick() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(flag).setSetting(Mockito.any(), Mockito.eq(true));
        Mockito.verify(panel).getInventory();
        Mockito.verify(addon).saveWorldSettings();
    }
}
