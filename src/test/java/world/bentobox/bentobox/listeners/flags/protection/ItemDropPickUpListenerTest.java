package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.Item;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

class ItemDropPickUpListenerTest extends CommonTestSetup {

    private ItemDropPickUpListener listener;
    @Mock
    private Item item;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(item.getLocation()).thenReturn(location);

        listener = new ItemDropPickUpListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnDropAllowed() {
        PlayerDropItemEvent e = new PlayerDropItemEvent(mockPlayer, item);
        listener.onDrop(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnDropNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerDropItemEvent e = new PlayerDropItemEvent(mockPlayer, item);
        listener.onDrop(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPickupAllowed() {
        EntityPickupItemEvent e = new EntityPickupItemEvent(mockPlayer, item, 0);
        listener.onPickup(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnPickupNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        EntityPickupItemEvent e = new EntityPickupItemEvent(mockPlayer, item, 0);
        listener.onPickup(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPickupNotPlayer() {
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        when(island.isAllowed(any(), any())).thenReturn(false);
        EntityPickupItemEvent e = new EntityPickupItemEvent(zombie, item, 0);
        listener.onPickup(e);
        assertFalse(e.isCancelled());
    }
}
