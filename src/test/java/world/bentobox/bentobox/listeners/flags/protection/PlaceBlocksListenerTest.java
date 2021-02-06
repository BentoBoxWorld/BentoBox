package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class, Tag.class} )
public class PlaceBlocksListenerTest extends AbstractCommonSetup {

    private PlaceBlocksListener pbl;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Listener
        pbl = new PlaceBlocksListener();
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceFire() {
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.FIRE);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlace() {
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.STONE);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        when(itemInHand.getType()).thenReturn(Material.STONE);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onHangingPlace(org.bukkit.event.hanging.HangingPlaceEvent)}.
     */
    @Test
    public void testOnHangingPlaceAllowed() {
        Hanging hanging = mock(Hanging.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        HangingPlaceEvent e = new HangingPlaceEvent(hanging, player, block, BlockFace.EAST);
        pbl.onHangingPlace(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onHangingPlace(org.bukkit.event.hanging.HangingPlaceEvent)}.
     */
    @Test
    public void testOnHangingPlaceNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Hanging hanging = mock(Hanging.class);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        HangingPlaceEvent e = new HangingPlaceEvent(hanging, player, block, BlockFace.EAST);
        pbl.onHangingPlace(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceNullItemInHand() {
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.STONE);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, null, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.STONE);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        when(itemInHand.getType()).thenReturn(Material.STONE);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     * Ensures that books are not protected by this listener.
     */
    @Test
    public void testOnBlockPlaceBook() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block placedBlock = mock(Block.class);
        when(placedBlock.getType()).thenReturn(Material.LECTERN);
        when(placedBlock.getLocation()).thenReturn(location);
        BlockState replacedBlockState = mock(BlockState.class);
        Block placedAgainst = mock(Block.class);
        ItemStack itemInHand = mock(ItemStack.class);
        when(itemInHand.getType()).thenReturn(Material.WRITTEN_BOOK);
        EquipmentSlot hand = EquipmentSlot.HAND;
        BlockPlaceEvent e = new BlockPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, player, true, hand);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), eq("protection.protected"));

        // With a WRITABLE BOOK now
        when(itemInHand.getType()).thenReturn(Material.WRITABLE_BOOK);
        pbl.onBlockPlace(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrameNotItemFrame() {
        Creeper creeper = mock(Creeper.class);
        when(creeper.getLocation()).thenReturn(location);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, creeper, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrame() {
        ItemFrame itemFrame = mock(ItemFrame.class);
        when(itemFrame.getType()).thenReturn(EntityType.ITEM_FRAME);
        when(itemFrame.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, itemFrame, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerHitItemFrame(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerHitItemFrameNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemFrame itemFrame = mock(ItemFrame.class);
        when(itemFrame.getType()).thenReturn(EntityType.ITEM_FRAME);
        when(itemFrame.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, itemFrame, EquipmentSlot.HAND);
        pbl.onPlayerHitItemFrame(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteract() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ARMOR_STAND, Material.FIREWORK_ROCKET, Material.ITEM_FRAME, Material.END_CRYSTAL, Material.CHEST, Material.TRAPPED_CHEST, Material.JUNGLE_BOAT);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.GRASS_BLOCK);
        for (int i = 0; i < 7; i++) {
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
            pbl.onPlayerInteract(e);
            assertEquals("Failed on " + item.getType().toString(), Result.ALLOW, e.useInteractedBlock());
        }
    }

    /**
     * Test method for {@link PlaceBlocksListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.ARMOR_STAND, Material.FIREWORK_ROCKET, Material.ITEM_FRAME, Material.END_CRYSTAL, Material.CHEST, Material.TRAPPED_CHEST, Material.DARK_OAK_BOAT);
        Block clickedBlock = mock(Block.class);
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.GRASS_BLOCK);
        for (int i = 0; i < 7; i++) {
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
            pbl.onPlayerInteract(e);
            assertEquals("Failed on " + item.getType().toString(), Result.DENY, e.useInteractedBlock());
        }
        verify(notifier, times(7)).notify(any(), eq("protection.protected"));
    }
}
