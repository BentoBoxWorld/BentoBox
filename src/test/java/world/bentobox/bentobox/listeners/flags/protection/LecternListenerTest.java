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

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

class LecternListenerTest extends CommonTestSetup {

    private LecternListener listener;
    @Mock
    private Lectern lectern;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(lectern.getLocation()).thenReturn(location);
        when(block.getLocation()).thenReturn(location);

        listener = new LecternListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnPlayerTakeBookAllowed() {
        PlayerTakeLecternBookEvent e = new PlayerTakeLecternBookEvent(mockPlayer, lectern);
        listener.onPlayerTakeBookFromLectern(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnPlayerTakeBookNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTakeLecternBookEvent e = new PlayerTakeLecternBookEvent(mockPlayer, lectern);
        listener.onPlayerTakeBookFromLectern(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPlaceWritableBookAllowed() {
        ItemStack writableBook = mock(ItemStack.class);
        when(writableBook.getType()).thenReturn(Material.WRITABLE_BOOK);
        BlockState blockState = mock(BlockState.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, block, writableBook, mockPlayer, true,
                EquipmentSlot.HAND);
        listener.onPlaceBooksOnLectern(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnPlaceWritableBookNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemStack writableBook = mock(ItemStack.class);
        when(writableBook.getType()).thenReturn(Material.WRITABLE_BOOK);
        BlockState blockState = mock(BlockState.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, block, writableBook, mockPlayer, true,
                EquipmentSlot.HAND);
        listener.onPlaceBooksOnLectern(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnPlaceWrittenBookNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemStack writtenBook = mock(ItemStack.class);
        when(writtenBook.getType()).thenReturn(Material.WRITTEN_BOOK);
        BlockState blockState = mock(BlockState.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, block, writtenBook, mockPlayer, true,
                EquipmentSlot.HAND);
        listener.onPlaceBooksOnLectern(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnPlaceNonBookNotAffected() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemStack stone = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE);
        BlockState blockState = mock(BlockState.class);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, block, stone, mockPlayer, true,
                EquipmentSlot.HAND);
        listener.onPlaceBooksOnLectern(e);
        assertFalse(e.isCancelled());
    }
}
