package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.FlagWorldSettingChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.util.Util;

public class WorldToggleClickTest extends CommonTestSetup {

    private WorldToggleClick listener;
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
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        flag = mock(Flag.class);
        when(flag.isSetForWorld(any())).thenReturn(false);

        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
