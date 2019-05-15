package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
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
     * @throws java.lang.Exception
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
        view = new MyView("name");

        type = SlotType.CONTAINER;
        click = ClickType.LEFT;
        inv = InventoryAction.UNKNOWN;

        // Panel Listener Manager
        plm = new PanelListenerManager();

        // Panel
        Optional<PanelListener> opl = Optional.of(pl);
        when(panel.getListener()).thenReturn(opl);
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
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

    class MyView extends InventoryView {

        private Inventory top;

        /**
         * @param name
         */
        @SuppressWarnings("deprecation")
        public MyView(String name) {
            top = mock(Inventory.class);
            when(top.getSize()).thenReturn(9);
            when(top.getHolder()).thenReturn(panel);
            when(top.getTitle()).thenReturn(name);
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

    }

    @After
    public void cleanUp() {
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
        Mockito.verify(player, Mockito.never()).closeInventory();
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
        Mockito.verify(player).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNoOpenPanels() {
        InventoryClickEvent e = new InventoryClickEvent(view, type, 0, click, inv);
        plm.onInventoryClick(e);
        // Nothing should happen
        Mockito.verify(player, Mockito.never()).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.PanelListenerManager#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOpenPanelsWrongPanel() {
        PanelListenerManager.getOpenPanels().put(uuid, panel);
        InventoryClickEvent e = new InventoryClickEvent(new MyView("another"), type, 0, click, inv);
        plm.onInventoryClick(e);
        // Panel should be removed
        assertTrue(PanelListenerManager.getOpenPanels().isEmpty());
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
        Mockito.verify(pl).onInventoryClick(Mockito.any(), Mockito.any());
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
        Mockito.verify(ch).onClick(Mockito.eq(panel), Mockito.any(User.class), Mockito.eq(click), Mockito.eq(0));
        Mockito.verify(pl).onInventoryClick(Mockito.any(), Mockito.any());
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
        assertTrue(PanelListenerManager.getOpenPanels().size() == 1);
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
        Mockito.verify(pl).onInventoryClose(event);

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
