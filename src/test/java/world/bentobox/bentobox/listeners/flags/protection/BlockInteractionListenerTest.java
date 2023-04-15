package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class BlockInteractionListenerTest extends AbstractCommonSetup {


    private EquipmentSlot hand;

    private BlockInteractionListener bil;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;

    private final Map<Material, Flag> inHandItems = new EnumMap<>(Material.class);

    private final Map<Material, Flag> clickedBlocks = new EnumMap<>(Material.class);

    private void setFlags() {
        inHandItems.put(Material.ENDER_PEARL, Flags.ENDER_PEARL);
        inHandItems.put(Material.BONE_MEAL, Flags.PLACE_BLOCKS);
        clickedBlocks.put(Material.DAMAGED_ANVIL, Flags.ANVIL);
        when(Tag.ANVIL.isTagged(Material.DAMAGED_ANVIL)).thenReturn(true);
        clickedBlocks.put(Material.BEACON, Flags.BEACON);
        clickedBlocks.put(Material.WHITE_BED, Flags.BED);
        when(Tag.BEDS.isTagged(Material.WHITE_BED)).thenReturn(true);
        clickedBlocks.put(Material.BREWING_STAND, Flags.BREWING);
        clickedBlocks.put(Material.WATER_CAULDRON, Flags.COLLECT_WATER);
        clickedBlocks.put(Material.BARREL, Flags.BARREL);
        clickedBlocks.put(Material.CHEST, Flags.CHEST);
        clickedBlocks.put(Material.CHEST_MINECART, Flags.CHEST);
        clickedBlocks.put(Material.TRAPPED_CHEST, Flags.TRAPPED_CHEST);
        clickedBlocks.put(Material.SHULKER_BOX, Flags.SHULKER_BOX);
        when(Tag.SHULKER_BOXES.isTagged(Material.SHULKER_BOX)).thenReturn(true);
        clickedBlocks.put(Material.FLOWER_POT, Flags.FLOWER_POT);
        clickedBlocks.put(Material.COMPOSTER, Flags.COMPOSTER);
        clickedBlocks.put(Material.DISPENSER, Flags.DISPENSER);
        clickedBlocks.put(Material.DROPPER, Flags.DROPPER);
        clickedBlocks.put(Material.HOPPER, Flags.HOPPER);
        clickedBlocks.put(Material.HOPPER_MINECART, Flags.HOPPER);
        clickedBlocks.put(Material.OAK_DOOR, Flags.DOOR);
        when(Tag.DOORS.isTagged(Material.OAK_DOOR)).thenReturn(true);
        clickedBlocks.put(Material.IRON_TRAPDOOR, Flags.TRAPDOOR);
        when(Tag.TRAPDOORS.isTagged(Material.IRON_TRAPDOOR)).thenReturn(true);
        clickedBlocks.put(Material.SPRUCE_FENCE_GATE, Flags.GATE);
        when(Tag.FENCE_GATES.isTagged(Material.SPRUCE_FENCE_GATE)).thenReturn(true);
        clickedBlocks.put(Material.BLAST_FURNACE, Flags.FURNACE);
        clickedBlocks.put(Material.CAMPFIRE, Flags.FURNACE);
        clickedBlocks.put(Material.FURNACE_MINECART, Flags.FURNACE);
        clickedBlocks.put(Material.FURNACE, Flags.FURNACE);
        clickedBlocks.put(Material.SMOKER, Flags.FURNACE);
        clickedBlocks.put(Material.ENCHANTING_TABLE, Flags.ENCHANTING);
        clickedBlocks.put(Material.ENDER_CHEST, Flags.ENDER_CHEST);
        clickedBlocks.put(Material.JUKEBOX, Flags.JUKEBOX);
        clickedBlocks.put(Material.NOTE_BLOCK, Flags.NOTE_BLOCK);
        clickedBlocks.put(Material.CRAFTING_TABLE, Flags.CRAFTING);
        clickedBlocks.put(Material.CARTOGRAPHY_TABLE, Flags.CRAFTING);
        clickedBlocks.put(Material.GRINDSTONE, Flags.CRAFTING);
        clickedBlocks.put(Material.STONECUTTER, Flags.CRAFTING);
        clickedBlocks.put(Material.LOOM, Flags.CRAFTING);
        clickedBlocks.put(Material.STONE_BUTTON, Flags.BUTTON);
        when(Tag.BUTTONS.isTagged(Material.STONE_BUTTON)).thenReturn(true);
        clickedBlocks.put(Material.LEVER, Flags.LEVER);
        clickedBlocks.put(Material.REPEATER, Flags.REDSTONE);
        clickedBlocks.put(Material.COMPARATOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DAYLIGHT_DETECTOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DRAGON_EGG, Flags.DRAGON_EGG);
        clickedBlocks.put(Material.END_PORTAL_FRAME, Flags.PLACE_BLOCKS);
        clickedBlocks.put(Material.ITEM_FRAME, Flags.ITEM_FRAME);
        clickedBlocks.put(Material.GLOW_ITEM_FRAME, Flags.ITEM_FRAME);
        clickedBlocks.put(Material.SWEET_BERRY_BUSH, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.CAVE_VINES, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.CAKE, Flags.CAKE);
        clickedBlocks.put(Material.BEEHIVE, Flags.HIVE);
        clickedBlocks.put(Material.BEE_NEST, Flags.HIVE);
    }


    /**
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Clicked block
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);

        // Inventory
        hand = EquipmentSlot.HAND;
        // Nothing in hand right now
        when(item.getType()).thenReturn(Material.AIR);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(inv.getItemInOffHand()).thenReturn(new ItemStack(Material.BUCKET));

        // FlagsManager
        setFlags();

        // Class under test
        bil = new BlockInteractionListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractItemFrameNotAllowed() {
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractItemFrameNotAllowedOtherFlagsOkay() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandPotsNotAllowed() {
        Arrays.stream(Material.values()).filter(m -> m.name().startsWith("POTTED")).forEach(bm -> {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertEquals("Failure " + bm, Event.Result.DENY, e.useInteractedBlock());
        });
        verify(notifier, times((int)Arrays.stream(Material.values()).filter(m -> m.name().startsWith("POTTED")).count())).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandNotAllowed() {
        int count = 0;
        int worldSettingCount = 0;
        for (Material bm : clickedBlocks.keySet()) {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertEquals("Failure " + bm, Event.Result.DENY, e.useInteractedBlock());
            if (clickedBlocks.get(bm).getType().equals(Type.PROTECTION)) {
                count++;
            } else if (clickedBlocks.get(bm).getType().equals(Type.WORLD_SETTING)) {
                worldSettingCount++;
            }
            verify(notifier, times(count)).notify(any(), eq("protection.protected"));
            verify(notifier, times(worldSettingCount)).notify(any(), eq("protection.world-protected"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNothingInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        for (Material bm : clickedBlocks.keySet()) {
            // Allow flags
            if (clickedBlocks.get(bm).getType().equals(Type.PROTECTION)) {
                when(island.isAllowed(any(), eq(clickedBlocks.get(bm)))).thenReturn(true);
            } else if (clickedBlocks.get(bm).getType().equals(Type.WORLD_SETTING)) {
                clickedBlocks.get(bm).setSetting(world, true);
            }
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertNotEquals("Failure " + bm, Event.Result.DENY, e.useInteractedBlock());
            verify(notifier, never()).notify(any(), eq("protection.protected"));
            verify(notifier, never()).notify(any(), eq("protection.world-protected"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandNotAllowed() {
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        assertEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertNotEquals(Event.Result.DENY, e.useInteractedBlock());
        assertNotEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractSpawnEggInHandOnItemFrameNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        assertEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
    }



    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Ignore("TODO")
    @Test
    public void testOnBlockBreak() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onDragonEggTeleport(org.bukkit.event.block.BlockFromToEvent)}.
     */
    @Ignore("TODO")
    @Test
    public void testOnDragonEggTeleport() {
        fail("Not yet implemented"); // TODO
    }

}
