package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockbukkit.mockbukkit.tags.MaterialTagMock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
class BlockInteractionListenerTest extends CommonTestSetup {
    private EquipmentSlot hand;

    private BlockInteractionListener bil;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;
    
    private Material itemFrame = Material.ITEM_FRAME;

    private final Map<Material, Flag> inHandItems = new HashMap<>();

    private final Map<Material, Flag> clickedBlocks = new HashMap<>();

    /**
     * Sets a Tag static field to a new MaterialTagMock containing the given materials.
     * Uses Unsafe to overwrite the static final field, same pattern as reinitTagFields().
     */
    @SuppressWarnings("java:S3011")
    private static void setTagField(String fieldName, Material... materials) {
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            java.lang.reflect.Field tagField = Tag.class.getDeclaredField(fieldName);
            tagField.setAccessible(true);
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(fieldName.toLowerCase(java.util.Locale.ROOT));
            MaterialTagMock newTag = new MaterialTagMock(key, materials);
            unsafe.putObject(unsafe.staticFieldBase(tagField), unsafe.staticFieldOffset(tagField), newTag);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set tag field " + fieldName, e);
        }
    }

    private void setFlags() {
        inHandItems.put(Material.ENDER_PEARL, Flags.ENDER_PEARL);
        inHandItems.put(Material.BONE_MEAL, Flags.PLACE_BLOCKS);
        // Tag-based materials (need tags populated via addToTag)
        clickedBlocks.put(Material.DAMAGED_ANVIL, Flags.ANVIL);
        clickedBlocks.put(Material.WHITE_BED, Flags.BED);
        clickedBlocks.put(Material.COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.WAXED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.EXPOSED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.WAXED_EXPOSED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.WEATHERED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.OXIDIZED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.COPPER_CHEST, Flags.CHEST);
        clickedBlocks.put(Material.SHULKER_BOX, Flags.SHULKER_BOX);
        clickedBlocks.put(Material.OAK_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.IRON_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.SPRUCE_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.STONE_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.ACACIA_WALL_HANGING_SIGN, Flags.SIGN_EDITING);
        clickedBlocks.put(Material.DARK_OAK_SIGN, Flags.SIGN_EDITING);
        clickedBlocks.put(Material.CHERRY_WALL_SIGN, Flags.SIGN_EDITING);
        // Switch-based materials (handled directly by material type)
        clickedBlocks.put(Material.BEACON, Flags.BEACON);
        clickedBlocks.put(Material.BREWING_STAND, Flags.BREWING);
        clickedBlocks.put(Material.WATER_CAULDRON, Flags.COLLECT_WATER);
        clickedBlocks.put(Material.BARREL, Flags.BARREL);
        clickedBlocks.put(Material.CHEST, Flags.CHEST);
        clickedBlocks.put(Material.CHEST_MINECART, Flags.CHEST);
        clickedBlocks.put(Material.TRAPPED_CHEST, Flags.TRAPPED_CHEST);
        clickedBlocks.put(Material.FLOWER_POT, Flags.FLOWER_POT);
        clickedBlocks.put(Material.COMPOSTER, Flags.COMPOSTER);
        clickedBlocks.put(Material.DISPENSER, Flags.DISPENSER);
        clickedBlocks.put(Material.DROPPER, Flags.DROPPER);
        clickedBlocks.put(Material.HOPPER, Flags.HOPPER);
        clickedBlocks.put(Material.HOPPER_MINECART, Flags.HOPPER);
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
        clickedBlocks.put(Material.LEVER, Flags.LEVER);
        clickedBlocks.put(Material.REPEATER, Flags.REDSTONE);
        clickedBlocks.put(Material.COMPARATOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DAYLIGHT_DETECTOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DRAGON_EGG, Flags.DRAGON_EGG);
        clickedBlocks.put(Material.END_PORTAL_FRAME, Flags.PLACE_BLOCKS);
        clickedBlocks.put(Material.ITEM_FRAME, Flags.ITEM_FRAME);
        clickedBlocks.put(Material.GLOW_ITEM_FRAME, Flags.ITEM_FRAME);
        clickedBlocks.put(Material.SWEET_BERRY_BUSH, Flags.HARVEST);
        clickedBlocks.put(Material.CAVE_VINES, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.CAKE, Flags.CAKE);
        clickedBlocks.put(Material.BEEHIVE, Flags.HIVE);
        clickedBlocks.put(Material.BEE_NEST, Flags.HIVE);
    }

    /**
     * Populates Tag fields with MaterialTagMock instances so that tag-based checks work in tests.
     */
    private void populateTags() {
        setTagField("ANVIL", Material.DAMAGED_ANVIL);
        setTagField("BEDS", Material.WHITE_BED);
        setTagField("COPPER_GOLEM_STATUES",
                Material.COPPER_GOLEM_STATUE, Material.WAXED_COPPER_GOLEM_STATUE,
                Material.EXPOSED_COPPER_GOLEM_STATUE, Material.WAXED_EXPOSED_COPPER_GOLEM_STATUE,
                Material.WEATHERED_COPPER_GOLEM_STATUE, Material.WAXED_WEATHERED_COPPER_GOLEM_STATUE,
                Material.OXIDIZED_COPPER_GOLEM_STATUE, Material.WAXED_OXIDIZED_COPPER_GOLEM_STATUE);
        setTagField("COPPER_CHESTS", Material.COPPER_CHEST);
        setTagField("SHULKER_BOXES", Material.SHULKER_BOX);
        setTagField("DOORS", Material.OAK_DOOR);
        setTagField("TRAPDOORS", Material.IRON_TRAPDOOR);
        setTagField("FENCE_GATES", Material.SPRUCE_FENCE_GATE);
        setTagField("BUTTONS", Material.STONE_BUTTON);
        setTagField("ALL_HANGING_SIGNS", Material.ACACIA_WALL_HANGING_SIGN);
        setTagField("SIGNS", Material.DARK_OAK_SIGN, Material.CHERRY_WALL_SIGN);
    }


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        System.out.println("setup");
        super.setUp();

        // Clicked block
        when(clickedBlock.getLocation()).thenReturn(location);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);

        // Inventory
        hand = EquipmentSlot.HAND;
        // Nothing in hand right now
        when(item.getType()).thenReturn(Material.AIR);
        when(mockPlayer.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(item);
        ItemStack mockBucket = mock(ItemStack.class);
        when(mockBucket.getType()).thenReturn(Material.BUCKET);
        when(inv.getItemInOffHand()).thenReturn(mockBucket);

        // FlagsManager
        setFlags();
        populateTags();

        // Class under test
        bil = new BlockInteractionListener();
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractItemFrameNotAllowed() {
        when(clickedBlock.getType()).thenReturn(itemFrame);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractItemFrameNotAllowedOtherFlagsOkay() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(itemFrame);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractNothingInHandPotsNotAllowed() {
        // Add all POTTED_ materials to Tag.FLOWER_POTS
        Material[] pottedMaterials = Arrays.stream(Material.values())
                .filter(m -> m.name().startsWith("POTTED"))
                .toArray(Material[]::new);
        setTagField("FLOWER_POTS", pottedMaterials);
        Arrays.stream(pottedMaterials).forEach(bm -> {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertEquals( Event.Result.DENY, e.useInteractedBlock(), "Failure " + bm);
        });
        verify(notifier, times(pottedMaterials.length)).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractNothingInHandNotAllowed() {
        int count = 0;
        int worldSettingCount = 0;
        // Make all block states a sign. Right now, only the sign check cares, so fix in the future if required
        Sign sign = mock(Sign.class);
        when(sign.isWaxed()).thenReturn(false);
        when(clickedBlock.getState()).thenReturn(sign);
        for (Material bm : clickedBlocks.keySet()) {
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertEquals(Event.Result.DENY, e.useInteractedBlock(), "Failure " + bm);
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
    void testOnPlayerInteractNothingInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        for (Material bm : clickedBlocks.keySet()) {
            // Allow flags
            if (clickedBlocks.get(bm).getType().equals(Type.PROTECTION)) {
                when(island.isAllowed(any(), eq(clickedBlocks.get(bm)))).thenReturn(true);
            } else if (clickedBlocks.get(bm).getType().equals(Type.WORLD_SETTING)) {
                clickedBlocks.get(bm).setSetting(world, true);
            }
            when(clickedBlock.getType()).thenReturn(bm);
            PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
            bil.onPlayerInteract(e);
            assertNotEquals(Event.Result.DENY, e.useInteractedBlock(), "Failure " + bm);
            verify(notifier, never()).notify(any(), eq("protection.protected"));
            verify(notifier, never()).notify(any(), eq("protection.world-protected"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractSpawnEggInHandNotAllowed() {
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        assertEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractSpawnEggInHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.SPAWNER);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertNotEquals(Event.Result.DENY, e.useInteractedBlock());
        assertNotEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier, never()).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnPlayerInteractSpawnEggInHandOnItemFrameNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(true);
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.ITEM_FRAME);
        when(item.getType()).thenReturn(Material.BLAZE_SPAWN_EGG);
        PlayerInteractEvent e = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST, hand);
        bil.onPlayerInteract(e);
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
        assertEquals(Event.Result.DENY, e.useItemInHand());
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
    }

}
