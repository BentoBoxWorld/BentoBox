package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, PlayerEvent.class, PlayerInteractEvent.class, Bukkit.class})
public class ObsidianScoopingListenerTest {

    @Mock
    private World world;
    private ObsidianScoopingListener listener;
    @Mock
    private ItemStack item;
    @Mock
    private Block clickedBlock;
    @Mock
    private BentoBox plugin;
    @Mock
    private Player who;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private LocalesManager lm;
    private Material inHand;
    private Material block;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Mock server
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PowerMockito.mockStatic(Bukkit.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);

        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        // Create new object
        listener = new ObsidianScoopingListener();

        // Mock player
        when(who.getWorld()).thenReturn(world);

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(who.getLocation()).thenReturn(location);

        when(who.getInventory()).thenReturn(mock(PlayerInventory.class));

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandWorld(Mockito.any())).thenReturn(world);
        when(iwm.getNetherWorld(Mockito.any())).thenReturn(world);
        when(iwm.getEndWorld(Mockito.any())).thenReturn(world);

        // Mock up IslandsManager
        when(plugin.getIslands()).thenReturn(im);

        // Mock up items and blocks
        when(clickedBlock.getX()).thenReturn(0);
        when(clickedBlock.getY()).thenReturn(0);
        when(clickedBlock.getZ()).thenReturn(0);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(item.getAmount()).thenReturn(1);

        // Users
        User.setPlugin(plugin);

        // Put player in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        // Put player on island
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        // Set as survival
        when(who.getGameMode()).thenReturn(GameMode.SURVIVAL);

        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // World settings Flag
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> map = new HashMap<>();
        map.put("OBSIDIAN_SCOOPING", true);
        when(ws.getWorldFlags()).thenReturn(map);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
        PlayerInteractEvent event = new PlayerInteractEvent(who, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST);
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
        PlayerInteractEvent event = new PlayerInteractEvent(who, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST);

        // Test different game modes
        for (GameMode gm : GameMode.values()) {
            when(who.getGameMode()).thenReturn(gm);
            if (!gm.equals(GameMode.SURVIVAL)) {
                assertFalse(listener.onPlayerInteract(event));
            }
        }
    }

    @Test
    public void testOnPlayerInteractSurvivalNotOnIsland() {
        PlayerInteractEvent event = new PlayerInteractEvent(who, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST);

        // Set as survival
        when(who.getGameMode()).thenReturn(GameMode.SURVIVAL);

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
        PlayerInteractEvent event = new PlayerInteractEvent(who, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.EAST);
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
