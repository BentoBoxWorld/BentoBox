package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

class GeoLimitClickListenerTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private TabbedPanel panel;
    @Mock
    private @NonNull Inventory inv;
    @Mock
    private GameModeAddon gma;

    private GeoLimitClickListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // IWM
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getGeoLimitSettings(any())).thenReturn(new ArrayList<>());
        when(iwm.getMobLimitSettings(any())).thenReturn(new ArrayList<>());
        // Panel
        when(panel.getInventory()).thenReturn(inv);
        when(panel.getWorld()).thenReturn(Optional.of(world));
        // User
        when(user.inWorld()).thenReturn(true);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        listener = new GeoLimitClickListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnClickNotInWorld() {
        when(user.inWorld()).thenReturn(false);
        assertTrue(listener.onClick(panel, user, ClickType.LEFT, 0));
        verify(user).sendMessage("general.errors.wrong-world");
        verify(user, never()).closeInventory();
    }

    @Test
    void testOnClickNoPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        assertTrue(listener.onClick(panel, user, ClickType.LEFT, 0));
        verify(user).sendMessage("general.errors.no-permission", "[permission]",
                "bskyblock.admin.settings.GEO_LIMIT_MOBS");
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    @Test
    void testOnClickWithPermission() {
        assertTrue(listener.onClick(panel, user, ClickType.LEFT, 0));
        verify(user, never()).sendMessage("general.errors.wrong-world");
        verify(user, never()).sendMessage(eq("general.errors.no-permission"), anyString(), anyString());
        verify(user).closeInventory();
    }
}
