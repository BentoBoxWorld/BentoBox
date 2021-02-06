package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class InventoryListenerTest extends AbstractCommonSetup {

    private final static List<Class<?>> HOLDERS = Arrays.asList(Horse.class, Chest.class,ShulkerBox.class, StorageMinecart.class,
            Dispenser.class,
            Dropper.class, Hopper.class, Furnace.class, BrewingStand.class,
            Villager.class, WanderingTrader.class);

    private InventoryListener l;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);

        // Listener
        l = new InventoryListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickAllowed() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getSize()).thenReturn(9);

        HOLDERS.forEach(c -> {
            Object holder = mock(c);
            when(inv.getHolder()).thenReturn((InventoryHolder) holder);
            when(view.getTopInventory()).thenReturn(inv);
            when(inv.getLocation()).thenReturn(location);
            when(view.getBottomInventory()).thenReturn(inv);
            SlotType slotType = SlotType.CONTAINER;
            InventoryAction action = InventoryAction.PICKUP_ONE;
            InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
            l.onInventoryClick(e);
            assertFalse(e.isCancelled());
        });

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNullHolder() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = null;
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNotPlayer() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(null);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        HOLDERS.forEach(c -> {
            Object holder = mock(c);
            when(inv.getHolder()).thenReturn((InventoryHolder) holder);
            when(view.getTopInventory()).thenReturn(inv);
            when(view.getBottomInventory()).thenReturn(inv);
            SlotType slotType = SlotType.CONTAINER;
            InventoryAction action = InventoryAction.PICKUP_ONE;
            InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
            l.onInventoryClick(e);
            assertTrue(e.isCancelled());
        });
        Mockito.verify(notifier, Mockito.times(HOLDERS.size())).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOtherHolderAllowed() {
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOtherHolderNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(InventoryHolder.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.InventoryListener#onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent)}.
     */
    @Test
    public void testOnInventoryClickOtherHolderPlayerNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        InventoryView view = mock(InventoryView.class);
        when(view.getPlayer()).thenReturn(player);
        Inventory inv = mock(Inventory.class);
        when(inv.getLocation()).thenReturn(location);
        when(inv.getSize()).thenReturn(9);
        InventoryHolder holder = mock(Player.class);
        when(inv.getHolder()).thenReturn(holder);
        when(view.getTopInventory()).thenReturn(inv);
        when(view.getBottomInventory()).thenReturn(inv);
        SlotType slotType = SlotType.CONTAINER;
        InventoryAction action = InventoryAction.PICKUP_ONE;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, 0, ClickType.LEFT, action );
        l.onInventoryClick(e);
        assertFalse(e.isCancelled());
    }


}
