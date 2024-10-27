package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Test class for PanelListenerManager.java
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class PanelListenerManagerTest {

    private static final String PANEL_NAME = "name";
    @Mock
    private Player player;
    private InventoryView view;
    @Mock
    private PanelListenerManager plm;
    @Mock
    private Panel panel;
    @Mock
    private Inventory anotherInv;
    @Mock
    private PanelListener pl;
    @Mock
    private ClickHandler ch;

    private UUID uuid;
    private SlotType type;
    private ClickType click;
    private InventoryAction inv;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Settings
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.isClosePanelOnClickOutside()).thenReturn(true);

        // Player
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        User.getInstance(player);

        // Inventory view
        view = new MyView(ChatColor.RED + PANEL_NAME);

        type = SlotType.CONTAINER;
        click = ClickType.LEFT;
        inv = InventoryAction.UNKNOWN;

        // Panel Listener Manager
        plm = new PanelListenerManager();

        // Panel
        Optional<PanelListener> opl = Optional.of(pl);
        when(panel.getListener()).thenReturn(opl);
        when(panel.getInventory()).thenReturn(view.getTopInventory());
        when(panel.getName()).thenReturn("name");
        Map<Integer, PanelItem> map = new HashMap<>();
        PanelItem panelItem = mock(PanelItem.class);

        // Click handler
        Optional<ClickHandler> och = Optional.of(ch);
        when(panelItem.getClickHandler()).thenReturn(och);
        map.put(0, panelItem);
        when(panel.getItems()).thenReturn(map);

        Panel wrongPanel = mock(Panel.class);
        when(wrongPanel.getName()).thenReturn("another_name");
        when(wrongPanel.getInventory()).thenReturn(anotherInv);

        // Clear the static panels
        PanelListenerManager.getOpenPanels().clear();
    }

    class MyView implements InventoryView {

        private final Inventory top;
        private final String name;

        /**
         */
        public MyView(String name) {
            top = mock(Inventory.class);
            when(top.getSize()).thenReturn(9);
            when(top.getHolder()).thenReturn(panel);
            this.name = name;
        }

        /**
         */
        public MyView(String name, Inventory inventory) {
            top = inventory;
            this.name = name;
        }

        @Override
        public Inventory getTopInventory() {
            return top;
        }

        @Override
        public Inventory getBottomInventory() {
            return null;
        }

        @Override
        public HumanEntity getPlayer() {
            return player;
        }

        @Override
        public InventoryType getType() {
            return InventoryType.PLAYER;
        }

        @Override
        public String getTitle() {
            return name;
        }

        @Override
        public String getOriginalTitle() {
            // TODO Auto-generated method stub
            return "";
        }

        @Override
        public void setTitle(String title) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            // TODO Auto-generated method stub

        }

        @Override
        public ItemStack getItem(int slot) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setCursor(ItemStack item) {
            // TODO Auto-generated method stub

        }

        @Override
        public ItemStack getCursor() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Inventory getInventory(int rawSlot) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int convertSlot(int rawSlot) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public SlotType getSlotType(int slot) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub

        }

        @Override
        public int countSlots() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean setProperty(Property prop, int value) {
            // TODO Auto-generated method stub
            return false;
        }


    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        PanelListenerManager.getOpenPanels().clear();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOutsideUnknownPanel() {
        SlotType type = SlotType.OUTSIDE;
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        verify(player, never()).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOutsideKnownPanel() {
        // Put a panel in the list
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        SlotType type = SlotType.OUTSIDE;
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        verify(player).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNoOpenPanels() {
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Nothing should happen
        verify(player, never()).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOpenPanelsWrongPanel() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Use another name for this panel
        InventoryView otherView = new MyView("another", panel.getInventory());
        InventoryClickEvent e = new InventoryClickEvent(otherView, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Panel should be removed
        assertTrue(PanelListenerManager.getOpenPanels().isEmpty());
        verify(player).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOpenPanelsDifferentColorPanel() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Use another name for this panel
        InventoryView otherView = new MyView(ChatColor.BLACK + PANEL_NAME, panel.getInventory());
        InventoryClickEvent e = new InventoryClickEvent(otherView, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Check that the onClick is called
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        verify(pl).onInventoryClick(any(), any());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOpenPanelsRightPanelWrongSlot() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Click on 1 instead of 0
        InventoryClickEvent e = new InventoryClickEvent(view, type, 1, click, inv);
        plm.onInventoryClick(e);
        assertTrue(e.isCancelled());
        verify(pl).onInventoryClick(any(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOpenPanelsRightPanelRightSlot() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Click on 0
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Check that the onClick is called
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        verify(pl).onInventoryClick(any(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent)}.
     */
    @Test
    public void testOnInventoryCloseNoPanels() {
        // Add a panel for another player
        PanelListenerManager.getOpenPanels().put(UUID.randomUUID(), panel);
        // No panels for this player
        InventoryCloseEvent event = new InventoryCloseEvent(view);
        plm.onInventoryClose(event);
        assertEquals(1, PanelListenerManager.getOpenPanels().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent)}.
     */
    @Test
    public void testOnInventoryClosePanels() {
        // Add a panel for player
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        InventoryCloseEvent event = new InventoryCloseEvent(view);
        plm.onInventoryClose(event);
        assertTrue(PanelListenerManager.getOpenPanels().isEmpty());
        verify(pl).onInventoryClose(event);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onLogOut(org.bukkit.event.player.PlayerQuitEvent)}.
     */
    @Test
    public void testOnLogOut() {
        // Add a panel for player
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Unknown player logs out

        Player unknown = mock(Player.class);
        when(unknown.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerQuitEvent event = new PlayerQuitEvent(unknown, "");
        plm.onLogOut(event);
        assertFalse(PanelListenerManager.getOpenPanels().isEmpty());

        // Real log out
        event = new PlayerQuitEvent(player, "");
        plm.onLogOut(event);
        assertTrue(PanelListenerManager.getOpenPanels().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#getOpenPanels()}.
     */
    @Test
    public void testGetOpenPanels() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        assertEquals(panel, PanelListenerManager.getOpenPanels().get(uuid));
    }

}
