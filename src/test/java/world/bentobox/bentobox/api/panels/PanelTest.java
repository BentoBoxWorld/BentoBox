package world.bentobox.bentobox.api.panels;

import static org.junit.Assert.assertEquals;
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
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.heads.HeadGetter;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, HeadGetter.class })
public class PanelTest {

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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Server & Bukkit
        Server server = mock(Server.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);

        name = "panel";
        items = Collections.emptyMap();
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());

        // Head getter
        PowerMockito.mockStatic(HeadGetter.class);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanel() {
        // Panel
        new Panel(name, items, 10, user, listener);

        // The next two lines have to be paired together to verify the static call
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.createInventory(eq(null), eq(18), eq(name));

        verify(listener).setup();
        verify(player).openInventory(any(Inventory.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanelZeroSize() {
        // Panel
        new Panel(name, items, 0, user, listener);

        // The next two lines have to be paired together to verify the static call
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.createInventory(eq(null), eq(9), eq(name));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanelTooBig() {
        // Panel
        new Panel(name, items, 100, user, listener);

        // The next two lines have to be paired together to verify the static call
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.createInventory(eq(null), eq(54), eq(name));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanelNullUser() {
        // Panel
        new Panel(name, items, 10, null, listener);
        verify(player, never()).openInventory(any(Inventory.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanelWithItems() {
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
        PowerMockito.verifyStatic(Bukkit.class, VerificationModeFactory.times(1));
        Bukkit.createInventory(eq(null), eq(54), eq(name));

        verify(inv, times(54)).setItem(anyInt(), eq(itemStack));
        verify(player).openInventory(any(Inventory.class));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#Panel(java.lang.String, java.util.Map, int, world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.panels.PanelListener)}.
     */
    @Test
    public void testPanelWithHeads() {
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

        // The next two lines have to be paired together to verify the static call
        PowerMockito.verifyStatic(HeadGetter.class, VerificationModeFactory.times(54));
        HeadGetter.getHead(eq(item), eq(p));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getInventory()}.
     */
    @Test
    public void testGetInventory() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(inv, p.getInventory());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getItems()}.
     */
    @Test
    public void testGetItems() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(items, p.getItems());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getListener()}.
     */
    @Test
    public void testGetListener() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(listener, p.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getUser()}.
     */
    @Test
    public void testGetUser() {
        Panel p = new Panel(name, items, 10, user, listener);
        assertEquals(user, p.getUser().get());

        p = new Panel(name, items, 10, null, listener);
        assertEquals(Optional.empty(), p.getUser());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#open(org.bukkit.entity.Player[])}.
     */
    @Test
    public void testOpenPlayerArray() {
        Panel p = new Panel(name, items, 10, user, listener);
        p.open(player, player, player);
        verify(player, times(4)).openInventory(inv);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#open(world.bentobox.bentobox.api.user.User[])}.
     */
    @Test
    public void testOpenUserArray() {
        Panel p = new Panel(name, items, 10, user, listener);
        p.open(user, user, user);
        verify(player, times(4)).openInventory(inv);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setInventory(org.bukkit.inventory.Inventory)}.
     */
    @Test
    public void testSetInventory() {
        Panel p = new Panel(name, items, 10, user, listener);
        Inventory inventory = mock(Inventory.class);
        p.setInventory(inventory);
        assertEquals(inventory, p.getInventory());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setItems(java.util.Map)}.
     */
    @Test
    public void testSetItems() {
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
    public void testSetListener() {
        Panel p = new Panel(name, items, 10, user, null);
        assertEquals(Optional.empty(), p.getListener());
        p.setListener(listener);
        assertEquals(listener, p.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setUser(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testSetUser() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(Optional.empty(), p.getUser());
        p.setUser(user);
        assertEquals(user, p.getUser().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#setHead(world.bentobox.bentobox.api.panels.PanelItem)}.
     */
    @Test
    public void testSetHead() {
        // Items
        ItemStack itemStack = mock(ItemStack.class);
        when(itemStack.getType()).thenReturn(Material.PLAYER_HEAD);
        ItemMeta im = mock(ItemMeta.class);
        when(im.getLocalizedName()).thenReturn("tastybento");
        when(itemStack.getItemMeta()).thenReturn(im);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(itemStack);
        when(item.isPlayerHead()).thenReturn(true);
        when(item.getName()).thenReturn("tastybento");

        items = new HashMap<>();
        for (int i = 0; i<10; i++) {
            items.put(i, item);
        }
        // Inv
        when(inv.getSize()).thenReturn(18);
        when(inv.getItem(anyInt())).thenReturn(itemStack);

        // Panel
        Panel p = new Panel(name, items, 0, user, listener);

        ItemStack itemStack2 = mock(ItemStack.class);
        when(itemStack2.getType()).thenReturn(Material.PLAYER_HEAD);
        ItemMeta im2 = mock(ItemMeta.class);
        when(im2.getLocalizedName()).thenReturn(ChatColor.WHITE + "" + ChatColor.BOLD + "tastybento");
        when(itemStack2.getItemMeta()).thenReturn(im2);

        PanelItem newItem = mock(PanelItem.class);
        when(itemStack.getType()).thenReturn(Material.PLAYER_HEAD);
        when(newItem.getItem()).thenReturn(itemStack2);
        when(newItem.isPlayerHead()).thenReturn(true);
        when(newItem.getName()).thenReturn("tastybento");

        p.setHead(newItem);

        assertEquals(newItem, p.getItems().get(0));
        verify(inv, times(18)).setItem(anyInt(), eq(itemStack2));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.Panel#getName()}.
     */
    @Test
    public void testGetName() {
        Panel p = new Panel(name, items, 10, null, listener);
        assertEquals(name, p.getName());
    }

}
