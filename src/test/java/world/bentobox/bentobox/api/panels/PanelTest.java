package world.bentobox.bentobox.api.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.heads.HeadGetter;

/**
 * @author tastybento
 *
 */
class PanelTest extends CommonTestSetup {

    private String name;
    private Map<Integer, PanelItem> items;
    @Mock
    private User user;
    @Mock
    private PanelListener listener;
    @Mock
    private Player player;
    @Mock
    private Inventory inv;
    private MockedStatic<HeadGetter> mockedHeadGetter;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(net.kyori.adventure.text.Component.class))).thenReturn(inv);

        name = "panel";
        items = Collections.emptyMap();
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());

        // Head getter
        mockedHeadGetter = Mockito.mockStatic(HeadGetter.class);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanel() {
        // Panel
        new Panel(name, items, 10, user, listener);

        // The next two lines have to be paired together to verify the static call
        mockedBukkit.verify(() ->  Bukkit.createInventory(eq(null), eq(18), any(net.kyori.adventure.text.Component.class)));

        verify(listener).setup();
        verify(player).openInventory(any(Inventory.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanelZeroSize() {
        // Panel
        new Panel(name, items, 0, user, listener);

        // The next two lines have to be paired together to verify the static call
        mockedBukkit.verify(() ->  Bukkit.createInventory(eq(null), eq(9), any(net.kyori.adventure.text.Component.class)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanelTooBig() {
        // Panel
        new Panel(name, items, 100, user, listener);

        // The next two lines have to be paired together to verify the static call
        mockedBukkit.verify(() ->  Bukkit.createInventory(eq(null), eq(54), any(net.kyori.adventure.text.Component.class)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanelNullUser() {
        // Panel
        new Panel(name, items, 10, null, listener);
        verify(player, never()).openInventory(any(Inventory.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanelWithItems() {
        // Items
        ItemStack itemStack = mock(ItemStack.class);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(itemStack);

        items = new HashMap<>();
        for (int i = 0; i<100; i++) {
            items.put(i, item);
        }
        // Panel
        new Panel(name, items, 0, user, listener);

        // The next two lines have to be paired together to verify the static call
        mockedBukkit.verify(() ->  Bukkit.createInventory(eq(null), eq(54), any(net.kyori.adventure.text.Component.class)));

        verify(inv, times(54)).setItem(anyInt(), eq(itemStack));
        verify(player).openInventory(any(Inventory.class));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testPanelWithHeads() {
        // Items
        ItemStack itemStack = mock(ItemStack.class);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(itemStack);
        when(item.isPlayerHead()).thenReturn(true);

        items = new HashMap<>();
        for (int i = 0; i<100; i++) {
            items.put(i, item);
        }
        // Panel
        Panel p = new Panel(name, items, 0, user, listener);

        mockedHeadGetter.verify(() -> HeadGetter.getHead(item, p), times(54));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getInventory()}.
     */
    @Test
    void testGetInventory() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(inv, p.getInventory());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getItems()}.
     */
    @Test
    void testGetItems() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(items, p.getItems());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getListener()}.
     */
    @Test
    void testGetListener() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertSame(listener, p.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getUser()}.
     */
    @Test
    void testGetUser() {
        Panel p = new Panel(name, items, 10, user, listener);
        assertSame(user, p.getUser().get());

        p = new Panel(name, items, 10, null, listener);
        assertEquals(Optional.empty(), p.getUser());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#open(org.bukkit.entity.Player[])}.
     */
    @Test
    void testOpenPlayerArray() {
        Panel p = new Panel(name, items, 10, user, listener);
        p.open(player, player, player);
        verify(player, times(4)).openInventory(inv);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#open(world.bentobox.bentobox.api.user.User[])}.
     */
    @Test
    void testOpenUserArray() {
        Panel p = new Panel(name, items, 10, user, listener);
        p.open(user, user, user);
        verify(player, times(4)).openInventory(inv);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setInventory(org.bukkit.inventory.Inventory)}.
     */
    @Test
    void testSetInventory() {
        Panel p = new Panel(name, items, 10, user, listener);
        Inventory inventory = mock(Inventory.class);
        p.setInventory(inventory);
        assertEquals(inventory, p.getInventory());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setItems(java.util.Map)}.
     */
    @Test
    void testSetItems() {
        Panel p = new Panel(name, items, 10, user, listener);
        Map<Integer, PanelItem> newMap = new HashMap<>();
        newMap.put(23, mock(PanelItem.class));
        p.setItems(newMap);
        assertEquals(newMap, p.getItems());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setListener(world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    void testSetListener() {
        Panel p = new Panel(name, items, 10, user, null);
        assertEquals(Optional.empty(), p.getListener());
        p.setListener(listener);
        assertSame(listener, p.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setUser(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    void testSetUser() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(Optional.empty(), p.getUser());
        p.setUser(user);
        assertSame(user, p.getUser().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getName()}.
     */
    @Test
    void testGetName() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(name, p.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#tryRefreshInPlace(String, Map, int)}.
     * A same-title, same-size refresh must update the existing inventory in place: it must not
     * create a new inventory nor re-open the panel (which would fire the InventoryClose/Open cascade).
     */
    @Test
    void testTryRefreshInPlaceSuccess() {
        ItemStack itemStack = mock(ItemStack.class);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(itemStack);
        // fixSize(10) == 18
        when(inv.getSize()).thenReturn(18);

        Panel p = new Panel(name, items, 10, user, listener);

        Map<Integer, PanelItem> newItems = new HashMap<>();
        newItems.put(0, item);

        boolean result = p.tryRefreshInPlace(name, newItems, 10);

        assertTrue(result);
        // Items map is swapped for the new one
        assertSame(newItems, p.getItems());
        // The inventory was NOT re-created and the panel was NOT re-opened: exactly the single
        // create/open from construction.
        mockedBukkit.verify(() -> Bukkit.createInventory(eq(null), anyInt(),
                any(net.kyori.adventure.text.Component.class)), times(1));
        verify(player, times(1)).openInventory(any(Inventory.class));
        // Slot 0 gets the new item; the remaining slots of the existing inventory are cleared
        verify(inv).setItem(0, itemStack);
        verify(inv).setItem(17, null);
        // Listener setup runs again after the in-place refresh (once on construct, once on refresh)
        verify(listener, times(2)).setup();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#tryRefreshInPlace(String, Map, int)}.
     * A title change (e.g. switching tab in a tabbed panel) cannot be reflected in place, so the
     * method must decline and let the caller re-open.
     */
    @Test
    void testTryRefreshInPlaceNameChangeReturnsFalse() {
        when(inv.getSize()).thenReturn(18);
        Panel p = new Panel(name, items, 10, user, listener);

        assertFalse(p.tryRefreshInPlace("a-different-title", items, 10));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#tryRefreshInPlace(String, Map, int)}.
     * A size change cannot be reflected in place, so the method must decline.
     */
    @Test
    void testTryRefreshInPlaceSizeChangeReturnsFalse() {
        when(inv.getSize()).thenReturn(18);
        Panel p = new Panel(name, items, 10, user, listener);

        // fixSize(19) == 27, which differs from the existing inventory size of 18
        assertFalse(p.tryRefreshInPlace(name, items, 19));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#tryRefreshInPlace(String, Map, int)}.
     * With auto-size (size == 0) the target size must be derived from the <b>incoming</b> items,
     * exactly like makePanel. If the refreshed contents would compute to a different inventory
     * size, an in-place refresh cannot represent that and must decline - otherwise the new items
     * would overflow the stale, smaller inventory.
     */
    @Test
    void testTryRefreshInPlaceAutoSizeDifferentSizeReturnsFalse() {
        ItemStack itemStack = mock(ItemStack.class);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(itemStack);

        // Built from 1 item with size == 0 -> fixSize(0) == 9
        Map<Integer, PanelItem> oneItem = new HashMap<>();
        oneItem.put(0, item);
        when(inv.getSize()).thenReturn(9);
        Panel p = new Panel(name, oneItem, 0, user, listener);

        // Refreshed contents fill 10 slots -> fixSize(0) == 18, differing from the existing 9.
        // Computed from the incoming items (not the old this.items), so this must decline.
        Map<Integer, PanelItem> tenItems = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            tenItems.put(i, item);
        }
        assertFalse(p.tryRefreshInPlace(name, tenItems, 0));
    }

}
