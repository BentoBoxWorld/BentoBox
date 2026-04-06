package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.LocalesManager;

class ObsidianScoopingListenerTest extends CommonTestSetup {

    private ObsidianScoopingListener listener;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;
    private Material inHand;
    private Material block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Create new object
        listener = new ObsidianScoopingListener();

        // Mock player
        when(mockPlayer.getWorld()).thenReturn(world);
        RayTraceResult rtr = mock(RayTraceResult.class);
        when(mockPlayer.rayTraceBlocks(5, FluidCollisionMode.ALWAYS)).thenReturn(rtr);
        when(rtr.getHitBlock()).thenReturn(clickedBlock);

        when(mockPlayer.getLocation()).thenReturn(location);

        when(mockPlayer.getInventory()).thenReturn(mock(PlayerInventory.class));

        // Worlds
        when(iwm.getIslandWorld(Mockito.any())).thenReturn(world);
        when(iwm.getNetherWorld(Mockito.any())).thenReturn(world);
        when(iwm.getEndWorld(Mockito.any())).thenReturn(world);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Mock up items and blocks
        when(clickedBlock.getX()).thenReturn(0);
        when(clickedBlock.getY()).thenReturn(0);
        when(clickedBlock.getZ()).thenReturn(0);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(clickedBlock.getRelative(any())).thenReturn(clickedBlock);
        when(item.getAmount()).thenReturn(1);

        // Users
        User.setPlugin(plugin);

        // Put player in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        // Put player on island
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        // Set as survival
        when(mockPlayer.getGameMode()).thenReturn(GameMode.SURVIVAL);

        // World settings Flag
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> map = new HashMap<>();
        map.put("OBSIDIAN_SCOOPING", true);
        when(ws.getWorldFlags()).thenReturn(map);

        PlayerInventory playerInventory = mock(PlayerInventory.class);
        when(playerInventory.getItemInMainHand()).thenReturn(item);
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(playerInventory.getItemInOffHand()).thenReturn(air);
        when(mockPlayer.getInventory()).thenReturn(playerInventory);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnPlayerInteract() {
        // Test incorrect items
        inHand = Material.ACACIA_DOOR;
        block = Material.BROWN_MUSHROOM;
        // Create the event
        testEvent();
    }

    @Test
    void testOnPlayerInteractBucketInHand() {
        // Test incorrect items
        inHand = Material.BUCKET;
        block = Material.BROWN_MUSHROOM;
        // Create the event
        testEvent();
    }

    @Test
    void testOnPlayerInteractObsidianAnvilInHand() {
        // Test with obsidian in hand
        inHand = Material.ANVIL;
        block = Material.OBSIDIAN;
        // Create the event
        testEvent();
    }

    @Test
    void testOnPlayerInteractObsidianBucketInHand() {
        // Positive test with 1 bucket in the stack
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;
        // Create the event
        testEvent();
    }

    @Test
    void testOnPlayerInteractObsidianManyBucketsInHand() {
        // Positive test with 1 bucket in the stack
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;

        // Positive test with 32 bucket in the stack
        when(item.getAmount()).thenReturn(32);
        // Create the event
        testEvent();
    }

    @Test
    void testOnPlayerInteractNotInWorld() {
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);
        // Test not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        assertFalse(listener.onPlayerInteract(event));
    }

    @Test
    void testOnPlayerInteractInWorld() {
        // Put player in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
    }

    @Test
    void testOnPlayerInteractGameModes() {
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);

        // Test different game modes
        for (GameMode gm : GameMode.values()) {
            when(mockPlayer.getGameMode()).thenReturn(gm);
            if (!gm.equals(GameMode.SURVIVAL)) {
                assertFalse(listener.onPlayerInteract(event));
            }
        }
    }

    @Test
    void testOnPlayerInteractSurvivalNotOnIsland() {
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);

        // Set as survival
        when(mockPlayer.getGameMode()).thenReturn(GameMode.SURVIVAL);

        // Positive test with 1 bucket in the stack
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);

        // Test when player is not on island
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(listener.onPlayerInteract(event));
    }

    @Test
    void testOnPlayerInteractCooldown() {
        // Set up for a successful scoop
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);

        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);

        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);

        // First scoop should succeed
        assertTrue(listener.onPlayerInteract(event));

        // Second scoop should fail due to cooldown
        assertFalse(listener.onPlayerInteract(event));
    }

    // --- Tests for BlockFormEvent (lava tip hologram) ---

    @Test
    void testObsidianFormNotObsidian() {
        BlockFormEvent event = createBlockFormEvent(Material.COBBLESTONE);
        assertFalse(listener.handleObsidianForm(event));
    }

    @Test
    void testObsidianFormNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertFalse(listener.handleObsidianForm(event));
    }

    @Test
    void testObsidianFormFlagDisabled() {
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> map = new HashMap<>();
        map.put("OBSIDIAN_SCOOPING", false);
        when(ws.getWorldFlags()).thenReturn(map);

        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertFalse(listener.handleObsidianForm(event));
    }

    @Test
    void testObsidianFormDurationDisabled() {
        // Set duration to 0 (disabled)
        plugin.getSettings().setObsidianScoopingLavaTipDuration(0);

        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertFalse(listener.handleObsidianForm(event));
    }

    @Test
    void testObsidianFormWithNearbyObsidian() {
        // Set up block with nearby obsidian
        Block obsidianBlock = mock(Block.class);
        when(obsidianBlock.getType()).thenReturn(Material.OBSIDIAN);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(obsidianBlock);

        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertFalse(listener.handleObsidianForm(event));
    }

    @Test
    void testObsidianFormSolitaryShowsHologram() {
        // Set duration explicitly so the scheduled tick count is deterministic
        plugin.getSettings().setObsidianScoopingLavaTipDuration(30);
        // Set up solitary obsidian (no nearby obsidian)
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);

        // Mock the locale manager to return a tip text
        LocalesManager localesManager = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(localesManager);
        when(localesManager.getOrDefault(any(String.class), any(String.class)))
                .thenReturn("<green>Scoop this up!</green>");

        // Mock TextDisplay spawning
        TextDisplay mockHologram = mock(TextDisplay.class);
        when(mockHologram.isValid()).thenReturn(true);
        when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenReturn(mockHologram);

        // Mock scheduler
        BukkitTask mockTask = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mockTask);

        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertTrue(listener.handleObsidianForm(event));

        // Verify hologram was spawned
        verify(world).spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class));
        // Verify a delayed removal task was scheduled (30 seconds = 600 ticks)
        verify(sch).runTaskLater(any(), any(Runnable.class), eq(600L));
    }

    @Test
    void testScoopingRemovesActiveHologramImmediately() {
        // Spawn a hologram via BlockFormEvent
        plugin.getSettings().setObsidianScoopingLavaTipDuration(30);
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);

        LocalesManager localesManager = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(localesManager);
        when(localesManager.getOrDefault(any(String.class), any(String.class)))
                .thenReturn("<green>Scoop this up!</green>");

        TextDisplay mockHologram = mock(TextDisplay.class);
        when(mockHologram.isValid()).thenReturn(true);
        when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenReturn(mockHologram);

        BukkitTask mockTask = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mockTask);

        BlockFormEvent formEvent = createBlockFormEvent(Material.OBSIDIAN);
        assertTrue(listener.handleObsidianForm(formEvent));

        // Now scoop the same obsidian block; clickedBlock must report the same Location key
        when(clickedBlock.getLocation()).thenReturn(location);
        when(item.getType()).thenReturn(Material.BUCKET);
        when(clickedBlock.getType()).thenReturn(Material.OBSIDIAN);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item,
                clickedBlock, BlockFace.EAST);
        assertTrue(listener.onPlayerInteract(interactEvent));

        // The scheduled task captured by runTask runs givePlayerLava synchronously in
        // production via Bukkit; in this test we invoke it manually.
        ArgumentCaptor<Runnable> runCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTask(any(), runCaptor.capture());
        runCaptor.getValue().run();

        // Hologram should have been removed immediately upon scooping
        verify(mockHologram).remove();
    }

    @Test
    void testObsidianFormEmptyTipText() {
        // Set up solitary obsidian
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);

        // Mock the locale manager to return empty text
        LocalesManager localesManager = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(localesManager);
        when(localesManager.getOrDefault(any(String.class), any(String.class))).thenReturn("");

        BlockFormEvent event = createBlockFormEvent(Material.OBSIDIAN);
        assertFalse(listener.handleObsidianForm(event));

        // Verify no hologram was spawned
        verify(world, never()).spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class));
    }

    @Test
    void testFindHologramLocationAbove() {
        Block above = mock(Block.class);
        when(above.getType()).thenReturn(Material.AIR);
        Location aboveLoc = mock(Location.class);
        when(above.getLocation()).thenReturn(aboveLoc);
        when(aboveLoc.add(0.5, 0.5, 0.5)).thenReturn(aboveLoc);
        when(above.isLiquid()).thenReturn(false);

        when(clickedBlock.getRelative(BlockFace.UP)).thenReturn(above);

        Location result = listener.findHologramLocation(clickedBlock);
        assertNotNull(result);
    }

    @Test
    void testFindHologramLocationSide() {
        // Above is solid
        Block solidBlock = mock(Block.class);
        when(solidBlock.getType()).thenReturn(Material.STONE);
        when(solidBlock.isLiquid()).thenReturn(false);
        when(clickedBlock.getRelative(BlockFace.UP)).thenReturn(solidBlock);

        // North is air
        Block northBlock = mock(Block.class);
        when(northBlock.getType()).thenReturn(Material.AIR);
        Location northLoc = mock(Location.class);
        when(northBlock.getLocation()).thenReturn(northLoc);
        when(northLoc.add(0.5, 0.5, 0.5)).thenReturn(northLoc);
        when(northBlock.isLiquid()).thenReturn(false);
        when(clickedBlock.getRelative(BlockFace.NORTH)).thenReturn(northBlock);

        Location result = listener.findHologramLocation(clickedBlock);
        assertNotNull(result);
    }

    @Test
    void testFindHologramLocationLiquid() {
        // Above is water (liquid) - should be valid
        Block waterBlock = mock(Block.class);
        when(waterBlock.getType()).thenReturn(Material.WATER);
        when(waterBlock.isLiquid()).thenReturn(true);
        Location waterLoc = mock(Location.class);
        when(waterBlock.getLocation()).thenReturn(waterLoc);
        when(waterLoc.add(0.5, 0.5, 0.5)).thenReturn(waterLoc);
        when(clickedBlock.getRelative(BlockFace.UP)).thenReturn(waterBlock);

        Location result = listener.findHologramLocation(clickedBlock);
        assertNotNull(result);
    }

    @Test
    void testFindHologramLocationAllBlocked() {
        // All surrounding blocks are solid
        Block solidBlock = mock(Block.class);
        when(solidBlock.getType()).thenReturn(Material.STONE);
        when(solidBlock.isLiquid()).thenReturn(false);
        when(clickedBlock.getRelative(any(BlockFace.class))).thenReturn(solidBlock);

        Location result = listener.findHologramLocation(clickedBlock);
        assertNull(result);
    }

    private BlockFormEvent createBlockFormEvent(Material newStateType) {
        Block formBlock = mock(Block.class);
        when(formBlock.getX()).thenReturn(0);
        when(formBlock.getY()).thenReturn(64);
        when(formBlock.getZ()).thenReturn(0);
        when(formBlock.getWorld()).thenReturn(world);
        when(formBlock.getLocation()).thenReturn(location);

        // Set up relative blocks for hologram placement
        Block airAbove = mock(Block.class);
        when(airAbove.getType()).thenReturn(Material.AIR);
        when(airAbove.isLiquid()).thenReturn(false);
        Location aboveLoc = mock(Location.class);
        when(airAbove.getLocation()).thenReturn(aboveLoc);
        when(aboveLoc.add(0.5, 0.5, 0.5)).thenReturn(aboveLoc);
        when(formBlock.getRelative(any(BlockFace.class))).thenReturn(airAbove);

        BlockState newState = mock(BlockState.class);
        when(newState.getType()).thenReturn(newStateType);

        return new BlockFormEvent(formBlock, newState);
    }

    private void testEvent() {
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        Block obsidianBlock = mock(Block.class);
        when(obsidianBlock.getType()).thenReturn(Material.OBSIDIAN);
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);

        ObsidianScoopingListener localListener = new ObsidianScoopingListener();
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);
        if (!item.getType().equals(Material.BUCKET)
                || !clickedBlock.getType().equals(Material.OBSIDIAN)) {
            assertFalse(localListener.onPlayerInteract(event));
        } else {
            // Test with obby close by in any of the possible locations
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        when(world.getBlockAt(x, y, z)).thenReturn(obsidianBlock);
                        assertFalse(localListener.onPlayerInteract(event));
                    }
                }
            }
            // Test where the area is free of obby
            when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);
            assertTrue(localListener.onPlayerInteract(event));
        }
    }
}
