package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.PlayerInventory;
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
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;

/**
 * Test class for PanelListenerManager.java
 * @author tastybento
 *
 */
class PanelListenerManagerTest extends CommonTestSetup {

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
        // Click handling now compares against the panel's cached plain name
        when(panel.getPlainName()).thenReturn("name");
        Map<Integer, PanelItem> map = new HashMap<>();
        PanelItem panelItem = mock(PanelItem.class);

        // Click handler
        Optional<ClickHandler> och = Optional.of(ch);
        when(panelItem.getClickHandler()).thenReturn(och);
        map.put(0, panelItem);
        when(panel.getItems()).thenReturn(map);

        Panel wrongPanel = mock(Panel.class);
        when(wrongPanel.getName()).thenReturn("another_name");
        when(wrongPanel.getPlainName()).thenReturn("another_name");
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
            // Not needed for test
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            // Not needed for test
        }

        @Override
        public ItemStack getItem(int slot) {
            
            return null;
        }

        @Override
        public void setCursor(ItemStack item) {
            // Not needed for test
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
            // Not needed for test
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
            // Not needed for test
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
    void testOnInventoryClickOutsideUnknownPanel() {
        SlotType localType = SlotType.OUTSIDE;
        InventoryClickEvent e = new InventoryClickEvent(view, localType, 0, click, inv);
        plm.onInventoryClick(e);
        verify(mockPlayer, never()).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    void testOnInventoryClickOutsideKnownPanel() {
        // Put a panel in the list
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        SlotType localType = SlotType.OUTSIDE;
        InventoryClickEvent e = new InventoryClickEvent(view, localType, 0, click, inv);
        plm.onInventoryClick(e);
        verify(mockPlayer).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    void testOnInventoryClickNoOpenPanels() {
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Nothing should happen
        verify(mockPlayer, never()).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    void testOnInventoryClickOpenPanelsWrongPanel() {
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
    void testOnInventoryClickOpenPanelsDifferentColorPanel() {
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
    void testOnInventoryClickOpenPanelsRightPanelWrongSlot() {
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
    void testOnInventoryClickOpenPanelsRightPanelRightSlot() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // Click on 0
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Check that the onClick is called
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        verify(pl).onInventoryClick(any(), any());
    }

    /**
     * A view whose raw slots 0-8 are the panel and 9 onwards are the player's inventory.
     */
    class SplitView extends MyView {

        private final Inventory bottom = mock(PlayerInventory.class);
        private ItemStack cursor;

        SplitView() {
            super(ChatColor.RED + PANEL_NAME, panel.getInventory());
        }

        @Override
        public Inventory getBottomInventory() {
            return bottom;
        }

        @Override
        public Inventory getInventory(int rawSlot) {
            return rawSlot < 9 ? getTopInventory() : bottom;
        }

        @Override
        public ItemStack getCursor() {
            return cursor;
        }

        @Override
        public void setCursor(ItemStack item) {
            cursor = item;
        }
    }

    private InventoryClickEvent clickPlayerInventory(InventoryAction action) {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        InventoryClickEvent e = new InventoryClickEvent(new SplitView(), type, 20, click, action);
        plm.onInventoryClick(e);
        return e;
    }

    @Test
    void testPlayerInventoryClickRoutedToListenerByDefault() {
        // A listener that has not opted in (e.g. an icon picker) still gets the click, cancelled
        InventoryClickEvent e = clickPlayerInventory(InventoryAction.PICKUP_ALL);
        assertTrue(e.isCancelled());
        verify(pl).onInventoryClick(any(), eq(e));
    }

    @Test
    void testPlayerInventoryClickAllowedWhenListenerOptsIn() {
        when(pl.allowsPlayerInventoryClicks()).thenReturn(true);
        InventoryClickEvent e = clickPlayerInventory(InventoryAction.PICKUP_ALL);
        assertFalse(e.isCancelled());
        verify(pl, never()).onInventoryClick(any(), any());
        verify(pl, never()).refreshPanel();
    }

    @Test
    void testPlayerInventoryClickAllowedWithoutListener() {
        when(panel.getListener()).thenReturn(Optional.empty());
        assertFalse(clickPlayerInventory(InventoryAction.DROP_ONE_SLOT).isCancelled());
        assertFalse(clickPlayerInventory(InventoryAction.PICKUP_FROM_BUNDLE).isCancelled());
    }

    @Test
    void testPlayerInventoryShiftAndDoubleClickBlocked() {
        when(panel.getListener()).thenReturn(Optional.empty());
        assertTrue(clickPlayerInventory(InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
        assertTrue(clickPlayerInventory(InventoryAction.COLLECT_TO_CURSOR).isCancelled());
    }

    @Test
    void testPanelClickWithCursorItemStaysCancelled() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        // The handler asks for the click not to be cancelled
        when(ch.onClick(any(), any(), any(), any(Integer.class))).thenReturn(false);
        SplitView splitView = new SplitView();
        ItemStack held = mock(ItemStack.class);
        when(held.getType()).thenReturn(Material.STONE);
        splitView.setCursor(held);
        InventoryClickEvent e = new InventoryClickEvent(splitView, type, 0, click, InventoryAction.PLACE_ALL);
        plm.onInventoryClick(e);
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        assertTrue(e.isCancelled());
    }

    @Test
    void testDragIntoPanelCancelled() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        InventoryDragEvent e = new InventoryDragEvent(new SplitView(), null, mock(ItemStack.class), false,
                Map.of(3, mock(ItemStack.class), 20, mock(ItemStack.class)));
        plm.onInventoryDrag(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testDragWithinPlayerInventoryAllowed() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        InventoryDragEvent e = new InventoryDragEvent(new SplitView(), null, mock(ItemStack.class), false,
                Map.of(20, mock(ItemStack.class), 21, mock(ItemStack.class)));
        plm.onInventoryDrag(e);
        assertFalse(e.isCancelled());
        assertEquals(Set.of(20, 21), e.getRawSlots());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent)}.
     */
    @Test
    void testOnInventoryCloseNoPanels() {
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
    void testOnInventoryClosePanels() {
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
    void testOnLogOut() {
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
    void testGetOpenPanels() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        assertSame(panel, PanelListenerManager.getOpenPanels().get(uuid));
    }

    /**
     * Test that clicks are blocked when TabbedPanel listener is on timeout
     */
    @Test
    void testOnInventoryClickTabbedPanelTimeout() {
        // Set up panel with a TabbedPanel listener
        TabbedPanel tabbedPanel = mock(TabbedPanel.class);
        when(tabbedPanel.hasClickCooldown()).thenReturn(true);
        when(panel.getListener()).thenReturn(Optional.of(tabbedPanel));
        when(tabbedPanel.isActionableSlot(0)).thenReturn(true);
        PanelListenerManager.getOpenPanels().put(uuid, panel);

        // Mock BentoBox.onTimeout to return true (on timeout)
        when(plugin.onTimeout(any(User.class), eq(panel), anyBoolean())).thenReturn(true);

        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);

        // Click handler and panel listener should NOT be called when on timeout
        verify(ch, never()).onClick(any(), any(), any(), eq(0));
        verify(tabbedPanel, never()).onInventoryClick(any(), any());
    }

    /**
     * Test that clicks proceed when TabbedPanel listener is not on timeout
     */
    @Test
    void testOnInventoryClickTabbedPanelNoTimeout() {
        // Set up panel with a TabbedPanel listener
        TabbedPanel tabbedPanel = mock(TabbedPanel.class);
        when(tabbedPanel.hasClickCooldown()).thenReturn(true);
        when(panel.getListener()).thenReturn(Optional.of(tabbedPanel));
        when(tabbedPanel.isActionableSlot(0)).thenReturn(true);
        PanelListenerManager.getOpenPanels().put(uuid, panel);

        // Mock BentoBox.onTimeout to return false (no timeout)
        when(plugin.onTimeout(any(User.class), eq(panel), anyBoolean())).thenReturn(false);

        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);

        // Click handler and panel listener should be called
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        verify(tabbedPanel).onInventoryClick(any(), any());
    }

    /**
     * Test that whether the clicked slot can do anything is passed to the cooldown check, so that a
     * client that sends several click packets for one physical click does not have the click that
     * matters thrown away. See #3049.
     */
    @Test
    void testOnInventoryClickTabbedPanelPassesSlotActionability() {
        TabbedPanel tabbedPanel = mock(TabbedPanel.class);
        when(tabbedPanel.hasClickCooldown()).thenReturn(true);
        when(panel.getListener()).thenReturn(Optional.of(tabbedPanel));
        PanelListenerManager.getOpenPanels().put(uuid, panel);

        // Slot 0 holds an item with a click handler, slot 1 does not
        when(tabbedPanel.isActionableSlot(0)).thenReturn(true);
        when(tabbedPanel.isActionableSlot(1)).thenReturn(false);

        plm.onInventoryClick(new InventoryClickEvent(view, type, 0, click, inv));
        verify(plugin).onTimeout(any(User.class), eq(panel), eq(true));

        plm.onInventoryClick(new InventoryClickEvent(view, type, 1, click, inv));
        verify(plugin).onTimeout(any(User.class), eq(panel), eq(false));
    }

    /**
     * Test that non-TabbedPanel listeners don't get timeout checks
     */
    @Test
    void testOnInventoryClickNonTabbedPanelNoTimeoutCheck() {
        // Regular panel listener (not TabbedPanel)
        PanelListenerManager.getOpenPanels().put(uuid, panel);

        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);

        // Click handler should be called regardless of timeout
        verify(ch).onClick(eq(panel), any(User.class), eq(click), eq(0));
        verify(pl).onInventoryClick(any(), any());
        // onTimeout should NOT be called for non-TabbedPanel
        verify(plugin, never()).onTimeout(any(User.class), any(Panel.class), anyBoolean());
    }

}
