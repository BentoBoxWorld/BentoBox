package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.TropicalFish;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class BucketListenerTest extends AbstractCommonSetup {


    private BucketListener l;

    /**
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Island manager
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Listener
        l = new BucketListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketEmpty(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketEmpty(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent)}.
     */
    @Test
    public void testOnBucketFillAllowed() {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketFillEvent e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.LAVA_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.MILK_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent)}.
     */
    @Test
    public void testOnBucketFillNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketFillEvent e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.LAVA_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.MILK_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        verify(notifier, times(4)).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent)}.
     */
    @Test
    public void testOnBucketFillMixedAllowed() {
        when(island.isAllowed(any(), eq(Flags.BUCKET))).thenReturn(false);
        when(island.isAllowed(any(), eq(Flags.COLLECT_WATER))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.COLLECT_LAVA))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.MILKING))).thenReturn(true);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(any())).thenReturn(block);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        PlayerBucketFillEvent e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertTrue(e.isCancelled());

        when(item.getType()).thenReturn(Material.LAVA_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        when(item.getType()).thenReturn(Material.MILK_BUCKET);
        e = new PlayerBucketFillEvent(player, block, block, BlockFace.UP, Material.WATER_BUCKET, item);
        l.onBucketFill(e);
        assertFalse(e.isCancelled());

        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingNotFish() {
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, player);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishNoWaterBucket() {
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishWaterBucket() {
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BucketListener#onTropicalFishScooping(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnTropicalFishScoopingFishWaterBucketNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        TropicalFish fish = mock(TropicalFish.class);
        when(fish.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(player, fish );
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.WATER_BUCKET);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        l.onTropicalFishScooping(e);
        assertTrue(e.isCancelled());

        verify(notifier).notify(any(), eq("protection.protected"));
    }
}
