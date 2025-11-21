package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

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

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.api.events.flags.FlagSettingChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

public class IslandToggleClickTest extends AbstractCommonSetup {

    private IslandToggleClick listener;
    @Mock
    private TabbedPanel panel;
    @Mock
    private User user;
    @Mock
    private Flag flag;
    @Mock
    private SettingsTab settingsTab;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
       super.setUp();

        // Island World Manager
         when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        listener = new IslandToggleClick("test");

        // Panel
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getPlayer()).thenReturn(mock(Player.class));
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));
        mockedUtil.when(() ->Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        FlagsManager fm = mock(FlagsManager.class);
        when(flag.isSetForWorld(any())).thenReturn(false);
        when(fm.getFlag(Mockito.anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island Manager
        when(island.getOwner()).thenReturn(uuid);
        when(im.getIsland(any(World.class), any(User.class))).thenReturn(island);

        // Optional island
        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getIslandAt(any())).thenReturn(opIsland);

        // Active tab
        when(panel.getActiveTab()).thenReturn(settingsTab);
        when(settingsTab.getIsland()).thenReturn(island);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.settings.test");
    }

    @Test
    public void testOnClick() {
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(island).toggleFlag(flag);
        verify(pim).callEvent(any(FlagSettingChangeEvent.class));
    }

    @Test
    public void testOnClickNoIsland() {
        when(settingsTab.getIsland()).thenReturn(null);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(island, never()).toggleFlag(flag);
    }

    @Test
    public void testOnClickNotOwner() {
        // No permission
        when(user.hasPermission(anyString())).thenReturn(false);
        // Pick a different UUID from owner
        UUID u = UUID.randomUUID();
        while(u.equals(uuid)) {
            u = UUID.randomUUID();
        }
        when(island.getOwner()).thenReturn(u);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(island, never()).toggleFlag(flag);
    }

}
