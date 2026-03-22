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

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

class LeashListenerTest extends CommonTestSetup {

    private LeashListener listener;
    @Mock
    private Entity entity;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(entity.getLocation()).thenReturn(location);
        when(block.getLocation()).thenReturn(location);

        listener = new LeashListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnLeashAllowed() {
        PlayerLeashEntityEvent e = new PlayerLeashEntityEvent(entity, mockPlayer, mockPlayer);
        listener.onLeash(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnLeashNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerLeashEntityEvent e = new PlayerLeashEntityEvent(entity, mockPlayer, mockPlayer);
        listener.onLeash(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnUnleashAllowed() {
        Wolf wolf = mock(Wolf.class);
        when(wolf.getLocation()).thenReturn(location);
        PlayerUnleashEntityEvent e = new PlayerUnleashEntityEvent(wolf, mockPlayer);
        listener.onUnleash(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnUnleashNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Wolf wolf = mock(Wolf.class);
        when(wolf.getLocation()).thenReturn(location);
        PlayerUnleashEntityEvent e = new PlayerUnleashEntityEvent(wolf, mockPlayer);
        listener.onUnleash(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPlayerLeashHitchAllowed() {
        LeashHitch leashHitch = mock(LeashHitch.class);
        when(leashHitch.getType()).thenReturn(EntityType.LEASH_KNOT);
        when(leashHitch.getLocation()).thenReturn(location);
        HangingPlaceEvent e = new HangingPlaceEvent(leashHitch, mockPlayer, block, BlockFace.EAST, EquipmentSlot.HAND);
        listener.onPlayerLeashHitch(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnPlayerLeashHitchNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        LeashHitch leashHitch = mock(LeashHitch.class);
        when(leashHitch.getType()).thenReturn(EntityType.LEASH_KNOT);
        when(leashHitch.getLocation()).thenReturn(location);
        HangingPlaceEvent e = new HangingPlaceEvent(leashHitch, mockPlayer, block, BlockFace.EAST, EquipmentSlot.HAND);
        listener.onPlayerLeashHitch(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPlayerLeashHitchNotLeashKnot() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Painting painting = mock(Painting.class);
        when(painting.getType()).thenReturn(EntityType.PAINTING);
        when(painting.getLocation()).thenReturn(location);
        HangingPlaceEvent e = new HangingPlaceEvent(painting, mockPlayer, block, BlockFace.EAST, EquipmentSlot.HAND);
        listener.onPlayerLeashHitch(e);
        assertFalse(e.isCancelled());
    }
}
