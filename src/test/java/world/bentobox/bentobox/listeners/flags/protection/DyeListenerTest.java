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

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Sheep;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

class DyeListenerTest extends CommonTestSetup {

    private DyeListener dl;
    @Mock
    private Block block;
    @Mock
    private ItemStack item;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Block is a sign
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getLocation()).thenReturn(location);

        // Item is a dye
        when(item.getType()).thenReturn(Material.RED_DYE);

        dl = new DyeListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnPlayerInteractSignDyeAllowed() {
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnPlayerInteractSignDyeNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPlayerInteractSignGlowInkSacNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        when(item.getType()).thenReturn(Material.GLOW_INK_SAC);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnPlayerInteractNotSign() {
        when(block.getType()).thenReturn(Material.STONE);
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnPlayerInteractNotDye() {
        when(item.getType()).thenReturn(Material.DIAMOND);
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnPlayerInteractLeftClick() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.LEFT_CLICK_BLOCK, item, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnPlayerInteractNullItem() {
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, null, block,
                null, EquipmentSlot.HAND);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSheepDyeAllowed() {
        Sheep sheep = mock(Sheep.class);
        when(sheep.getLocation()).thenReturn(location);
        SheepDyeWoolEvent e = new SheepDyeWoolEvent(sheep, DyeColor.RED, mockPlayer);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnSheepDyeNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Sheep sheep = mock(Sheep.class);
        when(sheep.getLocation()).thenReturn(location);
        SheepDyeWoolEvent e = new SheepDyeWoolEvent(sheep, DyeColor.RED, mockPlayer);
        dl.onPlayerInteract(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnSheepDyeNullPlayer() {
        Sheep sheep = mock(Sheep.class);
        when(sheep.getLocation()).thenReturn(location);
        when(island.isAllowed(any(), any())).thenReturn(false);
        SheepDyeWoolEvent e = new SheepDyeWoolEvent(sheep, DyeColor.RED, null);
        dl.onPlayerInteract(e);
        assertFalse(e.isCancelled());
    }
}
