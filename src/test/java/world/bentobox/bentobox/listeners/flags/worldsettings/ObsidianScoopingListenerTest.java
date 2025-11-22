package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.RayTraceResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.LocalesManager;

public class ObsidianScoopingListenerTest extends CommonTestSetup {

    private ObsidianScoopingListener listener;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;
    @Mock
    private LocalesManager lm;
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
    public void testOnPlayerInteract() {
        // Test incorrect items
        inHand = Material.ACACIA_DOOR;
        block = Material.BROWN_MUSHROOM;
        // Create the event
        testEvent();
    }

    @Test
    public void testOnPlayerInteractBucketInHand() {
        // Test incorrect items
        inHand = Material.BUCKET;
        block = Material.BROWN_MUSHROOM;
        // Create the event
        testEvent();
    }

    @Test
    public void testOnPlayerInteractObsidianAnvilInHand() {
        // Test with obsidian in hand
        inHand = Material.ANVIL;
        block = Material.OBSIDIAN;
        // Create the event
        testEvent();
    }

    @Test
    public void testOnPlayerInteractObsidianBucketInHand() {
        // Positive test with 1 bucket in the stack
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;
        // Create the event
        testEvent();
    }

    @Test
    public void testOnPlayerInteractObsidianManyBucketsInHand() {
        // Positive test with 1 bucket in the stack
        inHand = Material.BUCKET;
        block = Material.OBSIDIAN;

        // Positive test with 32 bucket in the stack
        when(item.getAmount()).thenReturn(32);
        // Create the event
        testEvent();
    }

    @Test
    public void testOnPlayerInteractNotInWorld() {
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);
        // Test not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        assertFalse(listener.onPlayerInteract(event));
    }

    @Test
    public void testOnPlayerInteractInWorld() {
        // Put player in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
    }

    @Test
    public void testOnPlayerInteractGameModes() {
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
    public void testOnPlayerInteractSurvivalNotOnIsland() {
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

    private void testEvent() {
        when(item.getType()).thenReturn(inHand);
        when(clickedBlock.getType()).thenReturn(block);
        Block obsidianBlock = mock(Block.class);
        when(obsidianBlock.getType()).thenReturn(Material.OBSIDIAN);
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);

        ObsidianScoopingListener listener = new ObsidianScoopingListener();
        PlayerInteractEvent event = new PlayerInteractEvent(mockPlayer, Action.RIGHT_CLICK_BLOCK, item, clickedBlock,
                BlockFace.EAST);
        if (!item.getType().equals(Material.BUCKET)
                || !clickedBlock.getType().equals(Material.OBSIDIAN)) {
            assertFalse(listener.onPlayerInteract(event));
        } else {
            // Test with obby close by in any of the possible locations
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        when(world.getBlockAt(Mockito.eq(x), Mockito.eq(y), Mockito.eq(z))).thenReturn(obsidianBlock);
                        assertFalse(listener.onPlayerInteract(event));
                    }
                }
            }
            // Test where the area is free of obby
            when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(airBlock);
            assertTrue(listener.onPlayerInteract(event));
        }
    }
}
