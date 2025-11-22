package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;

/**
 * Test class for PanelListenerManager.java
 * @author tastybento
 *
 */
public class PanelListenerManagerTest extends CommonTestSetup {

    private static final String PANEL_NAME = "name";
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

    private SlotType type;
    private ClickType click;
    private InventoryAction inv;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Settings
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.isClosePanelOnClickOutside()).thenReturn(true);

        // Player
        User.getInstance(mockPlayer);

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
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
            return mockPlayer;
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
            
            return "";
        }

        @Override
        public void setTitle(String title) {
            
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            

        }

        @Override
        public ItemStack getItem(int slot) {
            
            return null;
        }

        @Override
        public void setCursor(ItemStack item) {
            

        }

        @Override
        public ItemStack getCursor() {
            
            return null;
        }

        @Override
        public Inventory getInventory(int rawSlot) {
            return top;
        }

        @Override
        public int convertSlot(int rawSlot) {
            
            return 0;
        }

        @Override
        public SlotType getSlotType(int slot) {
            
            return null;
        }

        @Override
        public void close() {
            

        }

        @Override
        public int countSlots() {
            
            return 0;
        }

        @Override
        public boolean setProperty(Property prop, int value) {
            
            return false;
        }

        @Override
        public void open() {
            

        }

        @Override
        public @Nullable MenuType getMenuType() {
            
            return null;
        }

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOutsideUnknownPanel() {
        SlotType type = SlotType.OUTSIDE;
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        verify(mockPlayer, never()).closeInventory();
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
        verify(mockPlayer).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNoOpenPanels() {
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Nothing should happen
        verify(mockPlayer, never()).closeInventory();
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
        verify(mockPlayer).closeInventory();
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
        event = new PlayerQuitEvent(mockPlayer, "");
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
